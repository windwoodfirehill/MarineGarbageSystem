# Java 后端 + Python 模型服务

Spring Boot 负责 HTTP API、上传文件保存、模拟潮流粒子追踪、聚集预测和污染源溯源。Python 仅负责 YOLO 模型列表和推理。接口和响应结构与现有 Vue 前端兼容。潮流场和污染源仍是模拟数据。

## 启动

1. 准备 Java 17、Maven 3.9、Python，并安装 `backend/requirements.txt`。模型权重应在交接包 `04_模型权重` 目录。
2. 在 `backend` 目录运行 `python -m uvicorn app.model_api:app --host 127.0.0.1 --port 8001`。
3. 在 `java-backend` 目录运行 `mvn spring-boot:run`。
4. 在 `frontend` 目录首次运行 `npm install`，然后运行 `npm run dev`。

原 FastAPI 如占用 8000 需先停止。可通过 `ALGORITHM_BASE_URL` 设置模型服务地址、`UPLOAD_DIR` 设置图片保存路径，默认保存在 Java 启动目录的 `data/uploads`。现有实现未加鉴权，不要直接暴露到公网。

本机已提供编译后的 Java 程序；完成依赖安装后，在项目根目录运行 `powershell -ExecutionPolicy Bypass -File .\start_all.ps1` 可启动三个服务。本机 Python 虚拟环境位于 `D:\MarineGarbageEnv`（避免 Windows 路径长度限制）；也可通过环境变量 `MARINE_PYTHON` 指定 Python 可执行文件。
