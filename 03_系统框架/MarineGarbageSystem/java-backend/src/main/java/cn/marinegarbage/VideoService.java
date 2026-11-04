package cn.marinegarbage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
@Service
public class VideoService {
 private final Path root; private final String ffmpeg;
 public VideoService(@Value("${app.media-dir:./data/media}")String root,@Value("${app.ffmpeg:ffmpeg}")String ffmpeg)throws Exception{this.root=Path.of(root).toAbsolutePath();this.ffmpeg=ffmpeg;Files.createDirectories(this.root);Files.createDirectories(this.root.resolve("frames"));}
 public Path upload(MultipartFile file)throws Exception {String name=Objects.requireNonNullElse(file.getOriginalFilename(),"");int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot).toLowerCase();if(file.isEmpty()||!Set.of(".mp4",".mov",".mkv",".jpg",".jpeg",".png",".webp").contains(ext))throw new IllegalArgumentException("请选择视频或图片文件");Path path=root.resolve(UUID.randomUUID()+ext);file.transferTo(path);return path;}
 public Path frame(String source,double seconds)throws Exception{if(!Double.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("抽帧时间无效");Path frame=root.resolve("frames").resolve(UUID.randomUUID()+".jpg");var command=new ArrayList<String>(List.of(ffmpeg,"-hide_banner","-loglevel","error","-y"));if(!source.startsWith("rtsp://")&&!source.toLowerCase().matches(".*\\.(jpg|jpeg|png|webp)$"))command.addAll(List.of("-ss",Double.toString(seconds)));command.addAll(List.of("-i",source,"-frames:v","1",frame.toString()));Path log=Files.createTempFile("marine-frame-",".log");try{var process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("视频抽帧超时，请检查流是否在线");}if(process.exitValue()!=0||!Files.exists(frame)){System.err.println(Files.readString(log));throw new IllegalStateException("无法读取视频画面，请检查视频或推流状态");}return frame;}finally{Files.deleteIfExists(log);}}
 public Path resolve(String name,boolean frames){if(!name.matches("[a-f0-9-]+\\.(jpg|jpeg|png|webp|mp4|mov|mkv)"))throw new IllegalArgumentException("无效文件名");return (frames?root.resolve("frames"):root).resolve(name);}
}
