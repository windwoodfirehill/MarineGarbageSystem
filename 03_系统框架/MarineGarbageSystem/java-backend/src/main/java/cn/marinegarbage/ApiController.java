package cn.marinegarbage;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ApiController {
    private final String base; private final RestTemplate client; private final AnalysisService analysis; private final Path uploadDir;
    private static final Set<String> EXT=Set.of("jpg","jpeg","png","bmp","webp");
    public ApiController(@Value("${algorithm.base-url}") String base, @Value("${app.upload-dir}") String uploadDir, AnalysisService analysis) {
        this.base=base.replaceAll("/+$",""); this.analysis=analysis; this.uploadDir=Path.of(uploadDir).toAbsolutePath().normalize();
        var factory=new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(Duration.ofSeconds(5)); factory.setReadTimeout(Duration.ofMinutes(5)); this.client=new RestTemplate(factory);
    }
    @GetMapping("/") public Map<String,String> health() { return Map.of("service","marine-java-api","status","running"); }
    @GetMapping("/api/sources") public Map<String,Object> sources() { return analysis.sources(); }
    @GetMapping("/api/models") public ResponseEntity<?> models() { return call("/models",HttpMethod.GET,null); }
    @GetMapping("/static/uploads/{filename:.+}") public ResponseEntity<Resource> image(@PathVariable String filename) throws IOException {
        if(!filename.matches("[0-9a-f]{32}\\.(jpg|jpeg|png|bmp|webp)")) return ResponseEntity.notFound().build();
        Path path=uploadDir.resolve(filename).normalize(); if(!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        String ext=filename.substring(filename.lastIndexOf('.')+1); MediaType type=switch(ext) { case "png"->MediaType.IMAGE_PNG; case "jpg","jpeg"->MediaType.IMAGE_JPEG; case "bmp"->MediaType.parseMediaType("image/bmp"); default->MediaType.parseMediaType("image/webp"); };
        return ResponseEntity.ok().contentType(type).body(new UrlResource(path.toUri()));
    }
    @PostMapping(value={"/api/detect"},consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue="v4b") String model,
      @RequestParam(defaultValue="0.25") double conf, @RequestParam(defaultValue="0.45") double iou,
      @RequestParam(defaultValue="40") int track_steps, jakarta.servlet.http.HttpServletRequest request) throws IOException {
        String name=Objects.requireNonNullElse(file.getOriginalFilename(),""); int dot=name.lastIndexOf('.'); String ext=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        if(file.isEmpty() || !EXT.contains(ext)) return ResponseEntity.badRequest().body(Map.of("detail","请选择非空的 JPG、PNG、BMP 或 WebP 图片"));
        if(!Set.of("v4b","v4","v3").contains(model)) return ResponseEntity.badRequest().body(Map.of("detail","未知模型: "+model));
        if(!Double.isFinite(conf)||!Double.isFinite(iou)||conf<0||conf>1||iou<0||iou>1||track_steps<1||track_steps>1000) return ResponseEntity.badRequest().body(Map.of("detail","参数超出范围"));
        byte[] bytes=file.getBytes(); var body=new LinkedMultiValueMap<String,Object>();
        body.add("file",new ByteArrayResource(bytes){@Override public String getFilename(){return "upload."+ext;}});
        body.add("model",model); body.add("conf",Double.toString(conf)); body.add("iou",Double.toString(iou));
        var headers=new HttpHeaders(); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<?> response=call("/detect",HttpMethod.POST,new HttpEntity<>(body,headers));
        if(!response.getStatusCode().is2xxSuccessful()) return response;
        if(!(response.getBody() instanceof Map<?,?> raw)) return ResponseEntity.status(502).body(Map.of("detail","模型服务返回格式错误"));
        @SuppressWarnings("unchecked") Map<String,Object> det=(Map<String,Object>)raw;
        if(request.getRequestURI().endsWith("/detect")) return ResponseEntity.ok(Map.of("model",det.get("model"),"detection",det));
        Files.createDirectories(uploadDir); String filename=UUID.randomUUID().toString().replace("-","")+"."+ext;
        Files.write(uploadDir.resolve(filename),bytes,StandardOpenOption.CREATE_NEW);
        try { return ResponseEntity.ok(analysis.analyze(det,"/static/uploads/"+filename,track_steps)); }
        catch(RuntimeException e) { Files.deleteIfExists(uploadDir.resolve(filename)); throw e; }
    }
    private ResponseEntity<?> call(String path,HttpMethod method,HttpEntity<?> entity) {
        try { return client.exchange(base+path,method,entity,Map.class); }
        catch(HttpStatusCodeException e) { return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString()); }
        catch(ResourceAccessException e) { return ResponseEntity.status(503).body(Map.of("detail","模型服务不可用，请检查 Python 服务是否启动")); }
    }
}
