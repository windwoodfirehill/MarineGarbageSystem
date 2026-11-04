package cn.marinegarbage;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SourceService {
 private final MonitoringRepository records;
 public SourceService(MonitoringRepository records){this.records=records;}
 public Object status(String id,Map<String,String> body){String state=body.getOrDefault("status","SUSPECTED"),basis=body.getOrDefault("evidence","").trim();if(!Set.of("CONFIRMED_POLLUTION","SUSPECTED","CLEARED").contains(state))throw new IllegalArgumentException("无效点位状态");if(!state.equals("SUSPECTED")&&basis.isBlank())throw new IllegalArgumentException("确认状态需要填写核查依据");records.updateSourceStatus(id,state,basis);return Map.of("updated",true);}
 @Transactional public Object importPoints(List<Map<String,Object>> points){
 if(points.isEmpty()||points.size()>500)throw new IllegalArgumentException("每次导入 1–500 个点位");
 for(var p:points){
  String name=Objects.toString(p.get("name"),"").trim(),type=Objects.toString(p.get("source_type"),"候选点").trim();
  if(name.isEmpty()||name.length()>200||type.isEmpty()||type.length()>50)throw new IllegalArgumentException("点位名称和类型不能为空或过长");
  if(!(p.get("longitude") instanceof Number x)||!(p.get("latitude") instanceof Number y)||!Double.isFinite(x.doubleValue())||!Double.isFinite(y.doubleValue())||Math.abs(x.doubleValue())>180||Math.abs(y.doubleValue())>90)throw new IllegalArgumentException("点位经纬度无效: "+name);
  if(!(p.get("is_test") instanceof Boolean test))throw new IllegalArgumentException("请用 is_test 标明是否测试点: "+name);
  String source=Objects.toString(p.get("source_url"),"");if(!test&&!(source.startsWith("https://")||source.startsWith("http://")))throw new IllegalArgumentException("真实候选点必须提供公开出处 source_url");
  var row=new LinkedHashMap<String,Object>(p);row.put("name",name);row.put("source_type",type);row.put("evidence",Objects.toString(p.get("evidence"),test?"合成测试点，无实际排放证据":"导入候选点，未确认排放关系"));row.put("metadata",Map.of("is_test",test,"source_url",source,"verification","未确认","coordinate_datum",Objects.toString(p.get("coordinate_datum"),"未注明")));records.addSource(row);
 }
 return Map.of("imported",points.size());
 }
}
