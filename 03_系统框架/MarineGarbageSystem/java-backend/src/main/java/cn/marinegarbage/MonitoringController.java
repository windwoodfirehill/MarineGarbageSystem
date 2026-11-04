package cn.marinegarbage;
import java.util.*;
import java.nio.file.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.*;
import org.springframework.http.*;
@RestController
@RequestMapping("/api")
public class MonitoringController {
 private final MonitoringRepository records;private final VideoService videos;private final DetectionService detection;private final TraceService traces;private final CurrentFieldService currents;
 public MonitoringController(MonitoringRepository records,VideoService videos,DetectionService detection,TraceService traces,CurrentFieldService currents){this.records=records;this.videos=videos;this.detection=detection;this.traces=traces;this.currents=currents;}
 @GetMapping("/currents") public Object currents(){return currents.status();}
 @GetMapping("/candidate-sources") public Object sources(){return records.candidateSources();}
 @GetMapping("/records") public Object list(){return records.list();}
 @GetMapping("/records/{id}") public Object get(@PathVariable String id){return records.get(id);}
 @PutMapping("/records/{id}") public Object update(@PathVariable String id,@RequestBody Map<String,Object> body){if(body.get("title")==null||body.get("title").toString().isBlank())throw new IllegalArgumentException("请填写记录名称");records.update(id,body);return records.get(id);}
 @PostMapping("/videos/upload") public Object upload(@RequestParam MultipartFile file)throws Exception{Path path=videos.upload(file);String type=path.toString().matches(".*\\.(mp4|mov|mkv)$")?"VIDEO":"IMAGE";String id=records.create(file.getOriginalFilename(),type,path.toString());return records.get(id);}
 @PostMapping("/videos/streams") public Object stream(@RequestBody Map<String,String> body){String key=UUID.randomUUID().toString();String id=records.create(body.getOrDefault("title","无人机直播"),"STREAM","rtsp://127.0.0.1:8554/"+key);return Map.of("record",records.get(id),"push_path","/"+key,"rtmp_port",1935,"note","DJI 推流地址使用本机局域网 IP：rtmp://电脑IP:1935/"+key);}
 @PostMapping("/detections") public Object detect(@RequestBody Map<String,Object> body)throws Exception{String id=Objects.toString(body.get("monitoring_id"),"");var record=records.get(id);String model=Objects.toString(body.getOrDefault("model","v4b"));double seconds=((Number)body.getOrDefault("frame_seconds",0)).doubleValue();double conf=((Number)body.getOrDefault("conf",0.25)).doubleValue();Path frame=videos.frame(record.get("media_path").toString(),seconds);var result=detection.detect(frame,model,conf);return records.detection(id,model,seconds,frame.toString(),result);}
 @GetMapping("/records/{id}/detections")public Object history(@PathVariable String id){return records.detections(id);}
 @PostMapping("/traces")public Object trace(@RequestBody Map<String,Object> body){return traces.trace(Objects.toString(body.get("monitoring_id"),""),Objects.toString(body.get("garbage_type"),"Trash"),body);}
 @GetMapping("/media/frames/{name}")public ResponseEntity<Resource> frame(@PathVariable String name)throws Exception{Path p=videos.resolve(name,true);return Files.isRegularFile(p)?ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(new UrlResource(p.toUri())):ResponseEntity.notFound().build();}
}
