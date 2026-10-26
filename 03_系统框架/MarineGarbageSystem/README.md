# 近海漂浮垃圾自动监测与溯源分析系统

上海海洋大学大学生创新训练计划项目 · 牛俊晖团队
**基于无人机视觉与深度学习的近海漂浮垃圾自动监测与溯源分析系统**

## 系统架构（采集 → 识别 → 预测 → 溯源）

```
① 数据采集层   无人机影像（DJI + HDMI采集卡）→ 图片上传 API
② AI 识别层    YOLO 模型（多模型可选：v4b/v4/v3）→ 检测框/类别/置信度
③ 潮流预测层   FVCOM 潮流场 → 粒子追踪 → 垃圾迁移轨迹 + 聚集区域预测
④ 污染溯源层   GIS（河流入口/养殖区/港口）→ 污染源贡献度分析
⑤ 应用展示层   Vue 前端：识别结果可视化 + Leaflet 地图溯源
```

## 技术栈
- **后端**：Python FastAPI + ultralytics(YOLO) + numpy（复用训练环境 torch）
- **前端**：Vue 3 + Vite + Leaflet
- **模型**：训练成果 best.pt（v3 基线 / v4 负样本 / v4b 水面增强+mixup，均 nc=4：Glass/Metal/Plastic/Trash）

## 目录结构
```
MarineGarbageSystem/
├── backend/
│   ├── app/
│   │   ├── main.py           # FastAPI 入口（CORS + 静态目录）
│   │   ├── config.py         # 模型/类别/目录配置
│   │   ├── api/routes.py     # /api/models /sources /analyze /detect
│   │   ├── services/
│   │   │   ├── detector.py   # YOLO 多模型识别
│   │   │   ├── fvcom.py      # 潮流粒子追踪 + 聚集预测（模拟场）
│   │   │   └── source.py     # 污染源溯源（GIS 关联）
│   │   ├── models/schemas.py # Pydantic 数据模型
│   │   └── static/uploads/   # 上传图片
│   ├── requirements.txt
│   └── test_api.py           # API 全流程自测
└── frontend/
    ├── src/
    │   ├── App.vue           # 主界面（智能识别 / 地图溯源 tab）
    │   ├── api.js            # 后端 API 封装
    │   └── views/
    │       ├── DetectView.vue    # 上传识别 + canvas 画框
    │       └── TraceMapView.vue  # Leaflet 地图 + 溯源面板
    └── vite.config.js        # /api 代理到后端 8000
```

## 启动

**1. 后端**（复用训练环境 venv，依赖已装 fastapi/uvicorn/ultralytics）
```
cd backend
venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```
> 模型权重默认从交付包根目录的 `04_模型权重/` 加载，可在 `config.py` 中调整。

**2. 前端**
```
cd frontend
npm install
npm run dev        # http://127.0.0.1:5173
```

打开浏览器 http://127.0.0.1:5173 ，在「智能识别」上传近海水面垃圾图片 → 识别 → 切「地图溯源」查看聚集预测与污染源贡献度。

## 接口
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/models | 可用模型列表 |
| GET | /api/sources | 污染源点位 |
| POST | /api/analyze | 上传图片 → 识别+预测+溯源（multipart: file/model/conf） |
| POST | /api/detect | 仅识别 |

## 说明与待完善
- 潮流预测当前为**模拟潮流场示意数据**（粒子追踪），待接入真实 FVCOM 网格输出。
- 污染源点位为**模拟坐标**，待接入真实 GIS 河流入口/养殖区/港口数据。
- 无人机接入当前为「图片上传 API」形态，后续可扩展 HDMI 采集卡 / RTMP 实时流抽帧。
