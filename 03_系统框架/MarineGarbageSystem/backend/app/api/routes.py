# -*- coding: utf-8 -*-
"""API 路由：上传 / 识别 / 完整分析（识别->预测->溯源）"""
import os
import uuid
import shutil
from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse

from .. import config
from ..services import detector, fvcom, source

router = APIRouter(prefix="/api", tags=["marine-garbage"])


ALLOWED_EXT = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def _save_upload(file: UploadFile) -> str:
    """保存上传图片，返回绝对路径"""
    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in ALLOWED_EXT:
        raise HTTPException(400, f"不支持的图片格式: {ext or '未知'}")
    name = f"{uuid.uuid4().hex}{ext}"
    path = os.path.join(config.UPLOAD_DIR, name)
    with open(path, "wb") as f:
        shutil.copyfileobj(file.file, f)
    return path


@router.get("/models")
def get_models():
    """可用模型列表（前端下拉可选）"""
    return {"models": detector.list_models(),
            "default": config.DEFAULT_MODEL,
            "classes": config.CLASSES}


@router.get("/sources")
def get_sources():
    """污染源点位（溯源参考）"""
    return {"sources": source.POLLUTION_SOURCES,
            "note": "模拟点位，待接入真实GIS数据"}


@router.post("/analyze")
def analyze(
    file: UploadFile = File(...),
    model: str = Form(config.DEFAULT_MODEL),
    conf: float = Form(0.25),
    iou: float = Form(0.45),
    track_steps: int = Form(40),
):
    """完整流程：上传图片 -> YOLO识别 -> 潮流粒子追踪 -> 污染源溯源"""
    if model not in config.MODELS:
        raise HTTPException(400, f"未知模型: {model}")
    path = _save_upload(file)
    rel = os.path.join("uploads", os.path.basename(path)).replace("\\", "/")
    image_url = f"{config.BACKEND_URL}/static/{rel}"

    # 1) 识别
    try:
        det = detector.detect(path, model_key=model, conf=conf, iou=iou)
    except FileNotFoundError as e:
        raise HTTPException(500, str(e))

    width, height = det["image_size"]

    # 2) 潮流预测（粒子追踪 + 聚集区域）
    prediction = fvcom.run_particle_simulation(det["detections"], width, height,
                                               steps=track_steps)

    # 3) 污染源溯源
    trace = source.run_trace(det["detections"], prediction["aggregations"])

    return JSONResponse({
        "image": image_url,
        "model": det["model"],
        "conf": det["conf"],
        "detection": det,
        "prediction": prediction,
        "trace": trace,
    })


@router.post("/detect")
def detect_only(
    file: UploadFile = File(...),
    model: str = Form(config.DEFAULT_MODEL),
    conf: float = Form(0.25),
    iou: float = Form(0.45),
):
    """仅识别（不含潮流/溯源）"""
    if model not in config.MODELS:
        raise HTTPException(400, f"未知模型: {model}")
    path = _save_upload(file)
    try:
        det = detector.detect(path, model_key=model, conf=conf, iou=iou)
    except FileNotFoundError as e:
        raise HTTPException(500, str(e))
    return JSONResponse({"model": det["model"], "detection": det})
