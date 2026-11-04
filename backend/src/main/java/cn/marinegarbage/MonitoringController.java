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
 private final MonitoringRepository records;private final VideoService videos;private final DetectionService detection;private final TraceService traces;private final CurrentFieldService currents;private final SourceService sourceService;
 public MonitoringController(MonitoringRepository records,VideoService videos,DetectionService detection,TraceService traces,CurrentFieldService currents,SourceService sourceService){this.records=records;this.videos=videos;this.detection=detection;this.traces=traces;this.currents=currents;this.sourceService=sourceService;}
 @GetMapping("/currents") public Object currents(){return currents.status();}
 @PostMapping("/candidate-sources/import") public Object importSources(@RequestBody List<Map<String,Object>> points){return sourceService.importPoints(points);}
 @PutMapping("/candidate-sources/{id}/status") public Object status(@PathVariable String id,@RequestBody Map<String,String> body){return sourceService.status(id,body);}
 @GetMapping("/candidate-sources") public Object sources(){return records.candidateSources();}
 @GetMapping("/records") public Object list(){return records.list();}
 @GetMapping("/records/{id}") public Object get(@PathVariable String id){return records.get(id);}
 @PostMapping("/records/{id}/repredict") public Object repredict(@PathVariable String id,@RequestBody Map<String,String> body){var ds=records.detections(id);if(ds.isEmpty())throw new IllegalArgumentException("没有可用于预测的识别结果");String did=body.getOrDefault("detection_id",ds.get(0).get("id").toString());if(ds.stream().noneMatch(d->d.get("id").equals(did)))throw new IllegalArgumentException("识别结果不属于这条记录");var params=new LinkedHashMap<String,Object>();for(var t:records.traces(id)){@SuppressWarnings("unchecked") var old=(Map<String,Object>)t.get("parameters");if(did.equals(old.get("detection_id"))){for(String k:List.of("hours","include_test","observation_area"))if(old.get(k)!=null)params.put(k,old.get(k));if(old.get("captured_at")!=null)params.put("observation_time",old.get("captured_at"));break;}}@SuppressWarnings("unchecked") var chosen=(Map<String,Object>)ds.stream().filter(d->d.get("id").equals(did)).findFirst().orElseThrow().get("result");if(chosen.get("observation_snapshot") instanceof Map<?,?> snapshot){if(snapshot.get("captured_at")!=null)params.put("observation_time",snapshot.get("captured_at"));if(snapshot.get("observation_area")!=null)params.put("observation_area",snapshot.get("observation_area"));}params.put("detection_id",did);return traces.trace(id,"Trash",params);}
 @PostMapping("/records/{id}/copy") public Object copy(@PathVariable String id){var old=records.get(id);if(!Set.of("IMAGE","VIDEO").contains(old.get("media_type")))throw new IllegalArgumentException("直播需重新推流建立新连接");String fresh=records.create(old.get("title")+" · 重新检测",old.get("media_type").toString(),old.get("media_path").toString());var duplicated=new LinkedHashMap<String,Object>(old);duplicated.put("title",old.get("title")+" · 重新检测");records.update(fresh,duplicated);return records.get(fresh);}
 @PutMapping("/records/{id}") public Object update(@PathVariable String id,@RequestBody Map<String,Object> body){if(body.get("title")==null||body.get("title").toString().isBlank())throw new IllegalArgumentException("请填写记录名称");records.update(id,body);return records.get(id);}
 @PostMapping("/videos/upload") public Object upload(@RequestParam MultipartFile file)throws Exception{Path path=videos.upload(file);String type=path.toString().matches(".*\\.(mp4|mov|mkv)$")?"VIDEO":"IMAGE";String id=records.create(file.getOriginalFilename(),type,path.toString());return records.get(id);}
 @PostMapping("/videos/streams") public Object stream(@RequestBody Map<String,String> body){String key=UUID.randomUUID().toString();String id=records.create(body.getOrDefault("title","无人机直播"),"STREAM","rtsp://127.0.0.1:8554/live/"+key);return Map.of("record",records.get(id));}
 @GetMapping("/videos/streams/{id}/connection") public Object connection(@PathVariable String id)throws Exception{
 var record=records.get(id);if(!"STREAM".equals(record.get("media_type")))throw new IllegalArgumentException("该记录不是直播");
 String path=java.net.URI.create(record.get("media_path").toString()).getPath();int last=path.lastIndexOf('/');String key=path.substring(last+1),prefix=path.substring(0,last+1);
 List<Map<String,String>> hosts=new ArrayList<>();for(var nic:Collections.list(java.net.NetworkInterface.getNetworkInterfaces())){if(!nic.isUp()||nic.isLoopback())continue;String name=Objects.toString(nic.getDisplayName(),nic.getName());String lower=name.toLowerCase();if(nic.isVirtual()||lower.matches(".*(virtual|hyper-v|vmware|virtualbox|mihomo|clash|loopback).*"))continue;for(var addr:Collections.list(nic.getInetAddresses()))if(addr instanceof java.net.Inet4Address&&addr.isSiteLocalAddress())hosts.add(Map.of("address",addr.getHostAddress(),"interface",name));}
 hosts.sort(Comparator.comparingInt(h->h.get("interface").toLowerCase().matches(".*(wi-fi|wireless|wlan|802.11).* ".trim())?0:1));
 return Map.of("hosts",hosts,"rtmp_port",1935,"path_prefix",prefix,"stream_key",key,"note","遥控器与接收电脑连接同一局域网。RTMP 地址填上方地址，推流码填下方推流码。");
 }
 @PostMapping("/detections") public Object detect(@RequestBody Map<String,Object> body)throws Exception{String id=Objects.toString(body.get("monitoring_id"),"");var record=records.get(id);if("STREAM".equals(record.get("media_type")))throw new IllegalArgumentException("直播请先确认连接并开始，再通过截取入口识别");String model=Objects.toString(body.getOrDefault("model","v4b"));double seconds=((Number)body.getOrDefault("frame_seconds",0)).doubleValue();double conf=((Number)body.getOrDefault("conf",0.25)).doubleValue();Path frame=videos.frame(record.get("media_path").toString(),seconds);var result=detection.detect(frame,model,conf);return records.detection(id,model,seconds,frame.toString(),result);}
 @GetMapping("/records/{id}/detections")public Object history(@PathVariable String id){return records.detections(id);}
 @GetMapping("/records/{id}/traces") public Object traceHistory(@PathVariable String id){return records.traces(id);}
 @PostMapping("/traces")public Object trace(@RequestBody Map<String,Object> body){return traces.trace(Objects.toString(body.get("monitoring_id"),""),Objects.toString(body.get("garbage_type"),"Trash"),body);}
 @GetMapping("/media/frames/{name}")public ResponseEntity<Resource> frame(@PathVariable String name)throws Exception{Path p=videos.resolve(name,true);return Files.isRegularFile(p)?ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(new UrlResource(p.toUri())):ResponseEntity.notFound().build();}
}
