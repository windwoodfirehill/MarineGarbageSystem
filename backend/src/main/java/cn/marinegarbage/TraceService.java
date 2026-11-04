package cn.marinegarbage;
import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
@Service
public class TraceService {
 private final MonitoringRepository records;private final CurrentFieldService field;
 public TraceService(MonitoringRepository records,CurrentFieldService field){this.records=records;this.field=field;}
 private double n(Object x){return ((Number)x).doubleValue();}
 private double distance(double a,double b,double c,double d){double p=Math.toRadians(b),q=Math.toRadians(d),dl=Math.toRadians(c-a),dp=q-p;return 6371000*2*Math.asin(Math.min(1,Math.sqrt(Math.pow(Math.sin(dp/2),2)+Math.cos(p)*Math.cos(q)*Math.pow(Math.sin(dl/2),2))));}
 @SuppressWarnings("unchecked")
 public Object trace(String id,String type,Map<String,Object> parameters){
  var history=records.detections(id);if(history.isEmpty())throw new IllegalArgumentException("请先识别图片或视频帧，再进行预测");
  var latest=history.get(0);
  String detectionId=Objects.toString(parameters.getOrDefault("detection_id",latest.get("id")));
  var observation=history.stream().filter(d->d.get("id").equals(detectionId)).findFirst().orElseThrow(()->new IllegalArgumentException("识别记录不存在或不属于当前监控记录"));
  var det=(Map<String,Object>)observation.get("result");var detections=(List<Map<String,Object>>)det.get("detections");
  if(detections.isEmpty())throw new IllegalArgumentException("未识别到垃圾目标，无法选择主要类别预测");
  var counts=new TreeMap<String,Integer>();for(var d:detections)counts.merge(d.get("class").toString(),1,Integer::sum);
  var ordered=new ArrayList<>(counts.entrySet());ordered.sort(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
  List<Map<String,Object>> selectedClasses=new ArrayList<>();int chosen=0;for(var e:ordered){chosen+=e.getValue();selectedClasses.add(Map.of("type",e.getKey(),"count",e.getValue(),"share",e.getValue()/(double)detections.size()));if(chosen/(double)detections.size()>=.7)break;}
  type=selectedClasses.get(0).get("type").toString();
  var r=records.get(id);if((r.get("captured_at")==null&&parameters.get("observation_time")==null)||(r.get("observation_area")==null&&parameters.get("observation_area")==null))throw new IllegalArgumentException("请先保存拍摄时间和地图观测区域");
  if(!Set.of("Glass","Metal","Plastic","Trash").contains(type))throw new IllegalArgumentException("未知垃圾类别");
  double hours=n(parameters.getOrDefault("hours",24));if(!Double.isFinite(hours)||hours<1||hours>168)throw new IllegalArgumentException("回溯时长须在 1–168 小时之间");
  var area=(Map<String,Object>)parameters.getOrDefault("observation_area",r.get("observation_area"));double west=n(area.get("west")),east=n(area.get("east")),south=n(area.get("south")),north=n(area.get("north"));
  if(!(west<east&&south<north)||west< -180||east>180||south< -90||north>90)throw new IllegalArgumentException("观测范围无效");
  LocalDateTime dt=parameters.get("observation_time")!=null?LocalDateTime.parse(parameters.get("observation_time").toString()):r.get("captured_at") instanceof java.sql.Timestamp ts?ts.toLocalDateTime():LocalDateTime.parse(r.get("captured_at").toString().replace(' ','T'));
  if(parameters.get("observation_time")!=null)dt=LocalDateTime.parse(parameters.get("observation_time").toString());
  long time=dt.atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
  var params=new LinkedHashMap<String,Object>(parameters);params.put("observation_area",area);params.put("captured_at",dt.toString());params.put("timezone","Asia/Shanghai");params.put("seed_count",49);params.put("step_seconds",600);params.put("detection_id",detectionId);params.put("selected_classes",selectedClasses);params.put("cumulative_share",chosen/(double)detections.size());
  if(!field.covers(time-hours*3600,time))return records.saveTrace(id,type,"WAITING_FOR_DATA",params,Map.of("reason","拍摄时间及回溯时段不在已下载流场覆盖范围内","coverage",field.status()));
  List<List<List<Double>>> tracks=new ArrayList<>();int valid=0,completed=0;var sources=records.candidateSources().stream().filter(s->!"CLEARED".equals(((Map<?,?>)s.get("metadata")).get("status"))).filter(s->Boolean.TRUE.equals(parameters.get("include_test"))||!Boolean.TRUE.equals(((Map<?,?>)s.get("metadata")).get("is_test"))).toList();double[] minDistance=new double[sources.size()];Arrays.fill(minDistance,Double.POSITIVE_INFINITY);int[] encounters=new int[sources.size()];
  for(int iy=0;iy<7;iy++)for(int ix=0;ix<7;ix++){
   double lon=west+(east-west)*(ix+.5)/7,lat=south+(north-south)*(iy+.5)/7,t=time;var path=new ArrayList<List<Double>>();if(field.velocity(lon,lat,t)==null)continue;valid++;path.add(List.of(lon,lat));boolean[] hit=new boolean[sources.size()];
   for(int step=0;step<(int)Math.ceil(hours*6);step++){
    double h=Math.min(600,t-(time-hours*3600));if(h<=0)break;double[] v=field.velocity(lon,lat,t);if(v==null)break;
    double midLat=lat-v[1]*h/2/111195,midLon=lon-v[0]*h/2/(111195*Math.cos(Math.toRadians(lat)));double[] mid=field.velocity(midLon,midLat,t-h/2);if(mid==null)break;
    double nextLat=lat-mid[1]*h/111195,nextLon=lon-mid[0]*h/(111195*Math.cos(Math.toRadians(midLat)));if(field.velocity(nextLon,nextLat,t-h)==null)break;
    lat=nextLat;lon=nextLon;t-=h;path.add(List.of(lon,lat));
    for(int j=0;j<sources.size();j++){var s=sources.get(j);double d=distance(lon,lat,n(s.get("longitude")),n(s.get("latitude")));minDistance[j]=Math.min(minDistance[j],d);if(d<=5000)hit[j]=true;}
   }
   if(t<=time-hours*3600+1)completed++;for(int j=0;j<hit.length;j++)if(hit[j])encounters[j]++;tracks.add(path);
  }
  List<Map<String,Object>> comparison=new ArrayList<>();for(int j=0;j<sources.size();j++){var s=new LinkedHashMap<String,Object>(sources.get(j));s.put("encounter_particles",encounters[j]);s.put("closest_distance_km",Double.isFinite(minDistance[j])?Math.round(minDistance[j]/100.0)/10.0:null);comparison.add(s);}comparison.sort((a,b)->{int c=Integer.compare((Integer)b.get("encounter_particles"),(Integer)a.get("encounter_particles"));return c!=0?c:Double.compare(a.get("closest_distance_km")==null?Double.POSITIVE_INFINITY:n(a.get("closest_distance_km")),b.get("closest_distance_km")==null?Double.POSITIVE_INFINITY:n(b.get("closest_distance_km")));});
  var matched=comparison.stream().filter(s->(Integer)s.get("encounter_particles")>0).limit(3).map(s->s.get("name").toString()+(Boolean.TRUE.equals(((Map<?,?>)s.get("metadata")).get("is_test"))?"【测试点】":"")).toList();
  String conclusion=valid==0?"无有效海流网格，暂不能判断来源【未确认】":matched.isEmpty()?"未找到轨迹经过的候选点，暂不能判断来源【未确认】":"垃圾可能来自："+String.join("、",matched)+"【未确认】";
  Map<String,Object> result=new LinkedHashMap<>();result.put("selected_classes",selectedClasses);result.put("cumulative_share",chosen/(double)detections.size());result.put("detection_id",detectionId);result.put("include_test",Boolean.TRUE.equals(parameters.get("include_test")));result.put("tracks",tracks);result.put("candidate_sources",comparison);result.put("valid_particles",valid);result.put("completed_particles",completed);result.put("released_particles",49);result.put("flow",field.status());result.put("garbage_type",type);result.put("conclusion",conclusion);result.put("reason",valid==0?"观测区域没有可用海流网格，无法计算轨迹":"反向轨迹与候选点的接近程度已计算；不是排放认定或来源概率");result.put("limitations",List.of("HYCOM 全球流场，非 FVCOM 河口精细潮汐模型","表层被动粒子；未加入风、波浪、扩散、垃圾浮力差异","陆地、缺测或超出网格时停止，不穿越缺测区域","候选点 5km 邻域用于粗尺度筛查，不等于真实入海口边界","类别为辅助线索，当前不人为改变流速或来源权重"));
  return records.saveTrace(id,type,valid==0?"NO_VALID_WATER":"COMPLETED",params,result);
 }
}
