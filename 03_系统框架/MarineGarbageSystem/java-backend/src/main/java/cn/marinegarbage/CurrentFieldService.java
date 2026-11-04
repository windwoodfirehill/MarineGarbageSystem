package cn.marinegarbage;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
@Service
public class CurrentFieldService {
 private final ObjectMapper mapper;private JsonNode field;private double[] times,lats,lons;
 public CurrentFieldService(ObjectMapper mapper){this.mapper=mapper;}
 private synchronized boolean load(){if(field!=null)return true;Path p=Path.of("data/currents/hycom_2025q2.json");if(!Files.isRegularFile(p))return false;try{field=mapper.readTree(p.toFile());times=array(field.get("times"));lats=array(field.get("latitudes"));lons=array(field.get("longitudes"));return true;}catch(Exception e){field=null;return false;}}
 private double[] array(JsonNode n){double[] a=new double[n.size()];for(int i=0;i<a.length;i++)a[i]=n.get(i).asDouble();return a;}
 public Map<String,Object> status(){if(!load())return Map.of("available",false,"reason","历史流场正在下载或验证");return Map.of("available",true,"provider",field.get("provider").asText(),"start",(long)times[0],"end",(long)times[times.length-1],"west",lons[0],"east",lons[lons.length-1],"south",lats[0],"north",lats[lats.length-1],"source_url",field.get("source_url").asText());}
 public boolean covers(double start,double end){return load()&&start>=times[0]&&end<=times[times.length-1];}
 private int index(double[] a,double v){if(v<a[0]||v>a[a.length-1])return -1;int i=Arrays.binarySearch(a,v);if(i<0)i=-i-2;return Math.min(i,a.length-2);}
 public double[] velocity(double lon,double lat,double time){if(!load())return null;int x=index(lons,lon),y=index(lats,lat),t=index(times,time);if(x<0||y<0||t<0)return null;double fx=(lon-lons[x])/(lons[x+1]-lons[x]),fy=(lat-lats[y])/(lats[y+1]-lats[y]),ft=(time-times[t])/(times[t+1]-times[t]);double[] out=new double[2];for(int axis=0;axis<2;axis++){JsonNode f=field.get(axis==0?"u":"v");for(int dt=0;dt<2;dt++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){JsonNode v=f.get(t+dt).get(y+dy).get(x+dx);if(v.isNull())return null;out[axis]+=v.asDouble()*(dt==0?1-ft:ft)*(dy==0?1-fy:fy)*(dx==0?1-fx:fx);}}return out;}
}
