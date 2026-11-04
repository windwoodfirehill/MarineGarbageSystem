package cn.marinegarbage;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/live")
public class LiveController {
 private final LiveService live;public LiveController(LiveService live){this.live=live;}
 @GetMapping("/receiver")public Object receiver()throws Exception{var addresses=new ArrayList<String>();for(var nic:Collections.list(java.net.NetworkInterface.getNetworkInterfaces())){String name=Objects.toString(nic.getDisplayName(),"").toLowerCase();if(!nic.isUp()||nic.isLoopback()||nic.isVirtual()||name.matches(".*(virtual|hyper-v|vmware|virtualbox|mihomo|clash).*"))continue;for(var a:Collections.list(nic.getInetAddresses()))if(a instanceof java.net.Inet4Address&&a.isSiteLocalAddress())addresses.add(a.getHostAddress());}return Map.of("addresses",addresses);}
 @GetMapping("/sessions")public Object list(){return live.list();}
 @GetMapping("/sessions/{id}")public Object get(@PathVariable String id){return live.get(id);}
 @PostMapping("/sessions/{id}/confirm")public Object confirm(@PathVariable String id){return live.confirm(id);}
 @PostMapping("/sessions/{id}/reject")public Object reject(@PathVariable String id){return live.reject(id);}
 @PostMapping("/sessions/{id}/start")public Object start(@PathVariable String id,@RequestBody Map<String,String> body){return live.start(id,body.getOrDefault("mode","MANUAL"));}
 @PostMapping("/sessions/{id}/stop")public Object stop(@PathVariable String id){return live.stop(id);}
 @PostMapping("/sessions/{id}/capture")public Object capture(@PathVariable String id){return live.capture(id);}
}
