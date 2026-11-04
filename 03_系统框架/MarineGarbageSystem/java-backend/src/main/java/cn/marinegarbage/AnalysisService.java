package cn.marinegarbage;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
    private static final List<Map<String,Object>> SOURCES = List.of(
        Map.of("id","river_east","name","东部河流入海口","type","河流入口","x",60,"y",220,"weight",0.9),
        Map.of("id","river_west","name","西部河流入海口","type","河流入口","x",540,"y",60,"weight",0.7),
        Map.of("id","aqua","name","近岸养殖区","type","养殖区","x",300,"y",480,"weight",0.5),
        Map.of("id","port","name","渔港码头","type","港口","x",620,"y",380,"weight",0.6));
    public Map<String,Object> sources() { return Map.of("sources",SOURCES,"note","模拟点位，待接入真实GIS数据"); }
    private double num(Object v) { return ((Number)v).doubleValue(); }
    private double round(double v, int n) { double m=Math.pow(10,n); return Math.rint(v*m)/m; }
    @SuppressWarnings("unchecked")
    public Map<String,Object> analyze(Map<String,Object> det, String image, int steps) {
        List<Map<String,Object>> detections=(List<Map<String,Object>>)det.get("detections");
        List<Number> size=(List<Number>)det.get("image_size");
        double width=size.get(0).doubleValue(), height=size.get(1).doubleValue();
        List<List<List<Double>>> tracks=new ArrayList<>(); List<List<Double>> finals=new ArrayList<>();
        Map<String,List<List<Double>>> cells=new LinkedHashMap<>();
        for(var d:detections) {
            var center=(List<Number>)d.get("center"); double x=center.get(0).doubleValue(), y=center.get(1).doubleValue();
            List<List<Double>> path=new ArrayList<>(); path.add(List.of(x,y));
            for(int t=0;t<steps;t++) {
                double u=0.6+0.5*Math.sin(0.02*y+0.1*t)+0.2*Math.sin(0.01*x);
                double v=-0.3+0.4*Math.cos(0.02*x+0.1*t)-0.2*Math.cos(0.01*y);
                x=Math.min(Math.max(x+u,0),width); y=Math.min(Math.max(y+v,0),height);
                path.add(List.of(round(x,1),round(y,1)));
            }
            var last=path.get(path.size()-1); tracks.add(path); finals.add(last);
            String key=(int)Math.floor(last.get(0)/16)+":"+(int)Math.floor(last.get(1)/16);
            cells.computeIfAbsent(key,k->new ArrayList<>()).add(last);
        }
        List<Map<String,Object>> aggregations=new ArrayList<>();
        for(var points:cells.values()) if(points.size()>=2) {
            double sx=0,sy=0,minx=Double.POSITIVE_INFINITY,maxx=Double.NEGATIVE_INFINITY,miny=minx,maxy=maxx;
            for(var point:points) { double x=point.get(0), y=point.get(1); sx+=x; sy+=y; minx=Math.min(minx,x); maxx=Math.max(maxx,x); miny=Math.min(miny,y); maxy=Math.max(maxy,y); }
            aggregations.add(Map.of("center",List.of(round(sx/points.size(),1),round(sy/points.size(),1)),"radius",round(Math.max(maxx-minx,maxy-miny)/2+16,1),"count",points.size(),"density",round(points.size()/256.0,4)));
        }
        aggregations.sort((a,b)->Integer.compare((Integer)b.get("count"),(Integer)a.get("count")));
        Map<String,Object> prediction=Map.of("tracks",tracks,"final_positions",finals,"aggregations",aggregations,"note",detections.isEmpty()?"无检测目标，未执行潮流模拟":"模拟潮流场示意数据，待接入真实FVCOM网格");
        List<List<Number>> anchors=new ArrayList<>();
        if(!aggregations.isEmpty()) for(var a:aggregations) anchors.add((List<Number>)a.get("center"));
        else for(var d:detections) anchors.add((List<Number>)d.get("center"));
        Map<String,Object> trace=trace(anchors);
        return Map.of("image",image,"model",det.get("model"),"conf",det.get("conf"),"detection",det,"prediction",prediction,"trace",trace);
    }
    private Map<String,Object> trace(List<List<Number>> anchors) {
        if(anchors.isEmpty()) { Map<String,Object> result=new LinkedHashMap<>(); result.put("sources",List.of()); result.put("top_source",null); result.put("note","无目标位置，无法溯源"); return result; }
        List<Double> scores=new ArrayList<>(); double total=0;
        for(var s:SOURCES) { double sum=0; for(var p:anchors) sum+=num(s.get("weight"))*Math.exp(-Math.hypot(num(s.get("x"))-p.get(0).doubleValue(),num(s.get("y"))-p.get(1).doubleValue())/200); scores.add(sum); total+=sum; }
        List<Map<String,Object>> ranked=new ArrayList<>();
        for(int i=0;i<SOURCES.size();i++) { var s=SOURCES.get(i); ranked.add(Map.of("id",s.get("id"),"name",s.get("name"),"type",s.get("type"),"position",List.of(s.get("x"),s.get("y")),"contribution",round(100*scores.get(i)/(total==0?1:total),1))); }
        ranked.sort((a,b)->Double.compare(num(b.get("contribution")),num(a.get("contribution"))));
        return Map.of("sources",ranked,"top_source",ranked.get(0),"note","模拟污染源点位示意，待接入真实GIS数据");
    }
}
