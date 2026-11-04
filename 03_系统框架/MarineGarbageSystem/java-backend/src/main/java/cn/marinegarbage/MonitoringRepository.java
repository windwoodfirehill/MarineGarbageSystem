package cn.marinegarbage;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Repository
public class MonitoringRepository {
 private final JdbcTemplate db; private final ObjectMapper json;
 public MonitoringRepository(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Invalid JSON",e);}}
 private Map<String,Object> decode(Map<String,Object> row){ for(String key:List.of("metadata","observation_area","result","parameters")){Object value=row.get(key);if(value!=null)try{row.put(key,json.readValue(value.toString(),Object.class));}catch(Exception e){throw new IllegalStateException(e);}} return row; }
 public List<Map<String,Object>> candidateSources(){return db.queryForList("SELECT * FROM pollution_source ORDER BY source_type,name").stream().map(this::decode).toList();}
 public List<Map<String,Object>> list(){return db.queryForList("SELECT * FROM monitoring_record ORDER BY created_at DESC LIMIT 200").stream().map(this::decode).toList();}
 public Map<String,Object> get(String id){var rows=db.queryForList("SELECT * FROM monitoring_record WHERE id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"监控记录不存在");return decode(rows.get(0));}
 public String create(String title,String type,String path){String id=UUID.randomUUID().toString();db.update("INSERT INTO monitoring_record(id,title,media_type,media_path,metadata) VALUES(?,?,?,?,?)",id,title,type,path,"{}");return id;}
 public void update(String id,Map<String,Object> body){get(id);db.update("UPDATE monitoring_record SET title=?, captured_at=?, observation_area=?, metadata=? WHERE id=?",body.get("title"),body.get("captured_at"),body.get("observation_area")==null?null:encode(body.get("observation_area")),encode(body.getOrDefault("metadata",Map.of())),id);}
 public Map<String,Object> detection(String id,String model,double seconds,String path,Object result){String key=UUID.randomUUID().toString();db.update("INSERT INTO detection_record(id,monitoring_id,model,frame_seconds,image_path,result) VALUES(?,?,?,?,?,?)",key,id,model,seconds,path,encode(result));return Map.of("id",key,"monitoring_id",id,"frame_seconds",seconds,"image","/api/media/frames/"+java.nio.file.Path.of(path).getFileName(),"detection",result);}
 public List<Map<String,Object>> detections(String id){get(id);return db.queryForList("SELECT * FROM detection_record WHERE monitoring_id=? ORDER BY created_at DESC LIMIT 200",id).stream().map(this::decode).toList();}
 public Map<String,Object> saveTrace(String id,String type,String status,Map<String,Object> parameters,Object result){String key=UUID.randomUUID().toString();db.update("INSERT INTO trace_record(id,monitoring_id,garbage_type,status,parameters,result) VALUES(?,?,?,?,?,?)",key,id,type,status,encode(parameters),encode(result));return Map.of("id",key,"status",status,"result",result);}
 public Map<String,Object> blockedTrace(String id,String type,Map<String,Object> parameters){get(id);String key=UUID.randomUUID().toString();var result=Map.of("reason","尚未导入并验证该观测区域和时段的真实流场，暂停溯源计算","data_source","Copernicus / FVCOM 待验证");db.update("INSERT INTO trace_record(id,monitoring_id,garbage_type,status,parameters,result) VALUES(?,?,?,?,?,?)",key,id,type,"WAITING_FOR_DATA",encode(parameters),encode(result));return Map.of("id",key,"status","WAITING_FOR_DATA","result",result);}
}
