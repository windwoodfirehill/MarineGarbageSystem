"""YOLO 推理服务：只负责模型列表与图片识别。"""
import os
import tempfile
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from . import config
from .services import detector

app = FastAPI(title="YOLO inference service")

@app.get("/models")
def models():
    return {"models": detector.list_models(), "default": config.DEFAULT_MODEL, "classes": config.CLASSES}

@app.post("/detect")
def detect(file: UploadFile = File(...), model: str = Form(config.DEFAULT_MODEL), conf: float = Form(0.25), iou: float = Form(0.45)):
    if model not in config.MODELS:
        raise HTTPException(400, "未知模型")
    suffix = os.path.splitext(file.filename or "")[1].lower()
    if suffix not in {".jpg", ".jpeg", ".png", ".bmp", ".webp"}:
        raise HTTPException(400, "不支持的图片格式")
    import shutil
    path = None
    try:
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
            path = tmp.name
            shutil.copyfileobj(file.file, tmp)
        return detector.detect(path, model_key=model, conf=conf, iou=iou)
    except FileNotFoundError as e:
        raise HTTPException(500, str(e))
    finally:
        if path and os.path.exists(path):
            os.unlink(path)
