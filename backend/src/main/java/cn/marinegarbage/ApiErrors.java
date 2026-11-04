package cn.marinegarbage;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("detail",e.getMessage()));}
 @ExceptionHandler(IllegalStateException.class) public ResponseEntity<?> unavailable(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("detail",e.getMessage()));}
 @ExceptionHandler(RestClientException.class) public ResponseEntity<?> model(RestClientException e){return ResponseEntity.status(502).body(Map.of("detail","模型服务调用失败，请检查服务状态"));}
}
