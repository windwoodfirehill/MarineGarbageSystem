# -*- coding: utf-8 -*-
from ultralytics import YOLO
YOLO(r"C:\Users\93130\Desktop\WaterWasteDetection\runs\detect\yolo11n_v4_80\weights\last.pt").train(resume=True)
