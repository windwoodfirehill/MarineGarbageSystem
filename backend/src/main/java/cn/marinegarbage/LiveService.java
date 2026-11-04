package cn.marinegarbage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.*;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
@Service
public class LiveService {
 private final LiveSessionRepository sessions;private final MonitoringRepository records;private final VideoService video;private final DetectionService detection;private final TraceService trace;
 private final RestTemplate media;private final ExecutorService worker=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new SynchronousQueue<>(),new ThreadPoolExecutor.AbortPolicy());private final ConcurrentHashMap<String,AtomicBoolean> locks=new ConcurrentHashMap<>();private final ConcurrentHashMap<String,Instant> starts=new ConcurrentHashMap<>();
 private volatile Set<String> online=Set.of();private volatile String receiverError="";
 public LiveService(LiveSessionRepository sessions,MonitoringRepository records,VideoService video,DetectionService detection,TraceService trace){this.sessions=sessions;this.records=records;this.video=video;this.detection=detection;this.trace=trace;var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofSeconds(2));f.setReadTimeout(Duration.ofSeconds(3));media=new RestTemplate(f);sessions.recover();}
 @Scheduled(fixedDelay=2000) @SuppressWarnings("unchecked") public void discover(){try{Map<?,?> reply=media.getForObject("http://127.0.0.1:9997/v3/paths/list?itemsPerPage=100",Map.class);var active=new HashSet<String>();for(var raw:(List<Map<String,Object>>)reply.get("items")){if(!Boolean.TRUE.equals(raw.get("ready")))continue;String path=raw.get("name").toString();if(!path.matches("[A-Za-z0-9_/-]{1,200}")||path.contains(".."))continue;var source=(Map<?,?>)raw.get("source");if(source==null||source.get("id")==null)continue;String key=source.get("id").toString();active.add(key);sessions.discover(key,path);}online=Set.copyOf(active);receiverError="";for(var s:sessions.active())if(!active.contains(s.get("publisher_key").toString())&&!Set.of("DISCONNECTED","REJECTED").contains(s.get("state").toString())){sessions.disconnect(s.get("id").toString());starts.remove(s.get("id").toString());}}catch(Exception e){receiverError="接收服务暂不可用，请检查 MediaMTX";}}
 public Object list(){var rows=sessions.active().stream().filter(s->online.contains(s.get("publisher_key").toString())).toList();for(var row:rows)row.remove("latest_sample");return Map.of("sessions",rows,"receiver_error",receiverError);}
 public Object get(String id){return sessions.get(id);}
 public synchronized Object confirm(String id){var s=sessions.get(id);requireOnline(s);if(!"PENDING".equals(s.get("state")))throw new IllegalArgumentException("连接已处理");sessions.state(id,"CONFIRMED");return sessions.get(id);}
 public synchronized Object reject(String id){var s=sessions.get(id);if(!"PENDING".equals(s.get("state")))throw new IllegalArgumentException("只能忽略待确认连接");sessions.state(id,"REJECTED");return sessions.get(id);}
 public synchronized Object start(String id,String mode){var s=sessions.get(id);requireOnline(s);if(!Set.of("CONFIRMED","STOPPED").contains(s.get("state")))throw new IllegalArgumentException("请先确认连接或停止当前任务");if(!Set.of("MANUAL","AUTO").contains(mode))throw new IllegalArgumentException("截取模式无效");var record=records.get(s.get("monitoring_id").toString());if(record.get("observation_area")==null)throw new IllegalArgumentException("请先设置观测地点");var body=new LinkedHashMap<String,Object>(record);body.put("captured_at",LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0).toString().replace('T',' '));records.update(record.get("id").toString(),body);starts.remove(id);sessions.begin(id,mode);starts.putIfAbsent(id,Instant.now());return sessions.get(id);}
 public synchronized Object stop(String id){if(!"RUNNING".equals(sessions.get(id).get("state")))throw new IllegalArgumentException("该连接当前未运行");sessions.state(id,"STOPPED");return sessions.get(id);}
 private void requireOnline(Map<String,Object> s){if(!online.contains(s.get("publisher_key").toString()))throw new IllegalArgumentException("推流已断开");}
 public Object capture(String id){var s=sessions.get(id);if(!"RUNNING".equals(s.get("state"))||!"MANUAL".equals(s.get("mode")))throw new IllegalArgumentException("请先以手动模式开始");submit(s);return Map.of("accepted",true);}
 @Scheduled(fixedRate=1000) public void tick(){for(var s:sessions.active())if("RUNNING".equals(s.get("state"))&&"AUTO".equals(s.get("mode")))submit(s);}
 private void submit(Map<String,Object> s){String id=s.get("id").toString();AtomicBoolean lock=locks.computeIfAbsent(id,k->new AtomicBoolean());if(!lock.compareAndSet(false,true))return;if(worker instanceof ThreadPoolExecutor pool&&pool.getActiveCount()>=2){lock.set(false);return;}try{worker.submit(()->{try{process(s);}catch(Exception e){sessions.error(id,"截取或识别失败："+Objects.toString(e.getMessage(),"请检查服务"));}finally{lock.set(false);}});}catch(RejectedExecutionException e){lock.set(false);}}
 private void process(Map<String,Object> session)throws Exception{String id=session.get("id").toString();if(!"RUNNING".equals(sessions.get(id).get("state")))return;requireOnline(session);String monitor=session.get("monitoring_id").toString();var r=records.get(monitor);Instant runStarted=starts.getOrDefault(id,Instant.now());Path frame=video.frame(r.get("media_path").toString(),0);Instant captured=Instant.now();var det=detection.detect(frame,"v4b",.25);double elapsed=Duration.between(runStarted,captured).toMillis()/1000.0;var payload=new LinkedHashMap<String,Object>();det.forEach((k,v)->payload.put(k.toString(),v));payload.put("observation_snapshot",Map.of("captured_at",LocalDateTime.ofInstant(captured,ZoneId.of("Asia/Shanghai")).toString(),"observation_area",r.get("observation_area")));var saved=records.detection(monitor,"v4b",elapsed,frame.toString(),payload);
 var out=new LinkedHashMap<String,Object>();out.put("detection",saved);out.put("connection_id",id);out.put("captured_at",captured.toString());
 sessions.sample(id,out);}
 @jakarta.annotation.PreDestroy public void close(){worker.shutdownNow();}
}
