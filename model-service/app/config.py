# -*- coding: utf-8 -*-
"""系统全局配置：模型路径、类别、目录等"""
import os

# 后端根目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APP_DIR = os.path.dirname(BASE_DIR)
ROOT_DIR = os.path.dirname(APP_DIR)  # MarineGarbageSystem

# 模型权重目录（随交付包一起分发）
MODEL_DIR = os.path.abspath(os.environ.get("MODEL_DIR", os.path.join(APP_DIR, "models")))

# 可用模型注册表：key 为前端可选模型标识，value 为权重路径
# 训练历史：v3(基线) -> v4(负样本) -> v4b(水面增强+mixup)，均 nc=4
MODELS = {
    "v4b": os.path.join(MODEL_DIR, "v4b_best.pt"),
    "v4": os.path.join(MODEL_DIR, "v4_best.pt"),
    "v3": os.path.join(MODEL_DIR, "v3_best.pt"),
}

# 类别（与训练一致）
CLASSES = ["Glass", "Metal", "Plastic", "Trash"]
CLASS_CN = {"Glass": "玻璃", "Metal": "金属", "Plastic": "塑料", "Trash": "漂浮垃圾"}

# 默认模型
DEFAULT_MODEL = "v4b"

# 上传/结果目录
UPLOAD_DIR = os.path.join(APP_DIR, "static", "uploads")
RESULT_DIR = os.path.join(APP_DIR, "static", "results")
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(RESULT_DIR, exist_ok=True)

# 数据目录（潮流场、污染源点位等，初版用模拟/示例数据）
DATA_DIR = os.path.join(APP_DIR, "data")

# 后端服务地址（前端调用）
BACKEND_URL = "http://127.0.0.1:8000"
