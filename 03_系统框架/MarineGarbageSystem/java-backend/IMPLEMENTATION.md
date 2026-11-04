# 当前运行说明

前端使用 Vue + Vuetify，入口 http://localhost:5173 。Java 业务服务端口 8000，Python YOLO 服务端口 8001；MySQL 使用 marine_garbage 数据库，初始化语句保存在 sql/001_init.sql。

## 分层与接口

MonitoringController 编排 VideoService、DetectionService、TraceService；MonitoringRepository 负责数据库读写。

- POST /api/videos/upload：保存视频或图片并建档，不自动识别。
- POST /api/videos/streams：创建直播记录及 RTMP 路径。
- GET /api/records、GET/PUT /api/records/{id}：管理记录和元数据。
- POST /api/detections：monitoring_id、model、conf、frame_seconds，抽取指定帧识别并保存。
- GET /api/records/{id}/detections：读取历史识别。
- POST /api/traces：monitoring_id、garbage_type，当前记录 WAITING_FOR_DATA，不输出虚假来源排名。

Python 环境：D:\MarineGarbageEnv。媒体服务：D:\MarineGarbageTools\mediamtx.exe，需单独启动；RTMP 端口 1935，RTSP 端口 8554。无人机推流使用电脑局域网地址和界面创建的路径。

## 验证范围

图片识别、录像指定秒抽帧、FFmpeg 模拟 RTMP 推流抽帧均已通过实际 YOLO 和 MySQL 联调。尚未进行 DJI 实机测试。

## 暂停的真实溯源

目标区域 121.2–122.5°E、30.5–31.6°N。FVCOM 压缩包只有模型源码，没有历史流场。Copernicus 全球再分析产品有对应覆盖，但需账号下载，约 0.083° 日平均数据也不能直接代表河口精细潮汐。尚未取得并验证 NetCDF，按用户要求暂停真实溯源。

现有抽帧统计为单帧目标数量，不是跨帧去重总数。地图人工圈定观测区域，与无人机 GPS 分开。
