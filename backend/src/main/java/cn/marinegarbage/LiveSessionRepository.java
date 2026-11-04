package cn.marinegarbage;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Repository
public class LiveSessionRepository {
 private final JdbcTemplate db;private final ObjectMapper json;private final MonitoringRepository records;
 public LiveSessionRepository(JdbcTemplate db,ObjectMapper json,MonitoringRepository records){this.db=db;this.json=json;this.records=records;}
 private Map<String,Object> decode(Map<String,Object> r){if(r.get("latest_sample")!=null)try{r.put("latest_sample",json.readValue(r.get("latest_sample").toString(),Object.class));}catch(Exception e){throw new IllegalStateException(e);}return r;}
 public List<Map<String,Object>> list(){return db.queryForList("SELECT * FROM live_session ORDER BY created_at DESC LIMIT 100").stream().map(this::decode).toList();}
 public List<Map<String,Object>> active(){return db.queryForList("SELECT id,monitoring_id,publisher_key,stream_path,state,mode,started_at,created_at FROM live_session WHERE state IN ('PENDING','CONFIRMED','RUNNING','STOPPED') ORDER BY created_at DESC");}
 public Map<String,Object> get(String id){var rows=db.queryForList("SELECT * FROM live_session WHERE id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"连接不存在");return decode(rows.get(0));}
 public synchronized void discover(String key,String path){if(!db.queryForList("SELECT id FROM live_session WHERE publisher_key=?",key).isEmpty())return;String monitor=records.create("无人机连接 · "+path,"STREAM","rtsp://127.0.0.1:8554/"+path);db.update("INSERT INTO live_session(id,monitoring_id,publisher_key,stream_path) VALUES(?,?,?,?)",UUID.randomUUID().toString(),monitor,key,path);}
 public void state(String id,String state){db.update("UPDATE live_session SET state=? WHERE id=?",state,id);}
 public void begin(String id,String mode){db.update("UPDATE live_session SET state='RUNNING',mode=?,started_at=UTC_TIMESTAMP(3),last_error=NULL WHERE id=?",mode,id);}
 public void disconnect(String id){db.update("UPDATE live_session SET state='DISCONNECTED',ended_at=UTC_TIMESTAMP(3) WHERE id=?",id);}
 public void sample(String id,Object result){db.update("UPDATE live_session SET latest_sample=?,last_error=NULL WHERE id=?",records.encode(result),id);}
 public void error(String id,String message){db.update("UPDATE live_session SET last_error=? WHERE id=?",message,id);}
 public void recover(){db.update("UPDATE live_session SET state='STOPPED' WHERE state='RUNNING'");}
}
