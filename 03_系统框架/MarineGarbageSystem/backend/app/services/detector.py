# -*- coding: utf-8 -*-
"""YOLO 识别服务：加载多模型，支持切换模型/阈值推理"""
import os
from ultralytics import YOLO
from .. import config

# 模型缓存（懒加载，避免启动即占显存）
_model_cache = {}


def list_models():
    """返回可用模型及其权重状态"""
    out = []
    for key, path in config.MODELS.items():
        out.append({"id": key, "weight": path, "exists": os.path.exists(path)})
    return out


def _get_model(key):
    if key not in _model_cache:
        path = config.MODELS.get(key)
        if not path or not os.path.exists(path):
            raise FileNotFoundError(f"模型 [{key}] 权重不存在: {path}")
        _model_cache[key] = YOLO(path)
    return _model_cache[key]


def detect(image_path, model_key=None, conf=0.25, iou=0.45, max_det=300):
    """单张图片识别，返回检测结果 JSON"""
    key = model_key or config.DEFAULT_MODEL
    model = _get_model(key)
    results = model.predict(
        source=image_path, conf=conf, iou=iou,
        max_det=max_det, verbose=False, device=0,
    )
    r = results[0]
    boxes = r.boxes
    detections = []
    if boxes is not None and len(boxes) > 0:
        names = r.names
        for i in range(len(boxes)):
            cls_id = int(boxes.cls[i].item())
            conf_i = round(float(boxes.conf[i].item()), 4)
            xyxy = [round(float(v), 1) for v in boxes.xyxy[i].tolist()]  # x1 y1 x2 y2
            x, y, w, h = boxes.xywh[i].tolist()  # 中心点 + 宽高（像素）
            detections.append({
                "class_id": cls_id,
                "class": names[cls_id],
                "class_cn": config.CLASS_CN.get(names[cls_id], names[cls_id]),
                "confidence": conf_i,
                "bbox_xyxy": xyxy,
                "center": [round(float(x), 1), round(float(y), 1)],
                "size": [round(float(w), 1), round(float(h), 1)],
            })
    return {
        "model": key,
        "conf": conf,
        "iou": iou,
        "num_detections": len(detections),
        "image_size": [r.orig_shape[1], r.orig_shape[0]],  # w, h
        "detections": detections,
    }
