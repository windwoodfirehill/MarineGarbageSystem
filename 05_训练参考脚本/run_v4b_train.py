# -*- coding: utf-8 -*-
from ultralytics import YOLO
m = YOLO("yolo11n.pt")
m.train(data=r"C:\Users\93130\Desktop\datasets\Data_v4\data_v4.yaml",
        imgsz=640, epochs=80, batch=16,
        optimizer="MuSGD", patience=40, close_mosaic=10, seed=42,
        flipud=0.5,                 # 水面镜像增强(漂浮物垂直翻转)
        hsv_h=0.015, hsv_s=0.7, hsv_v=0.5,  # 亮度/饱和抖动模拟水面耀斑
        mixup=0.1,                  # 特征混合增强，提升Trash等难类多样性
        project=r"C:\Users\93130\Desktop\WaterWasteDetection\runs\detect",
        name="yolo11n_v4b", exist_ok=True, device=0, workers=0)
