package cn.marinegarbage;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
@Service
public class DetectionService {
 private final RestTemplate http; private final String base;
 public DetectionService(@Value("${algorithm.base-url}")String base){this.base=base;var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofSeconds(5));f.setReadTimeout(Duration.ofMinutes(3));http=new RestTemplate(f);}
 public Map<?,?> detect(Path frame,String model,double conf){if(!java.util.Set.of("v4b","v4","v3").contains(model)||!Double.isFinite(conf)||conf<0||conf>1)throw new IllegalArgumentException("模型或置信度无效");var form=new LinkedMultiValueMap<String,Object>();form.add("file",new FileSystemResource(frame));form.add("model",model);form.add("conf",Double.toString(conf));var h=new HttpHeaders();h.setContentType(MediaType.MULTIPART_FORM_DATA);return http.postForObject(base+"/detect",new HttpEntity<>(form,h),Map.class);}
}
