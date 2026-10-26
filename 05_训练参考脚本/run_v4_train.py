from ultralytics import YOLO
m = YOLO("yolo11n.pt")
m.train(data=r"C:\Users\93130\Desktop\datasets\Data_v4\data_v4.yaml", imgsz=640, epochs=80, batch=16,
         optimizer="MuSGD", patience=40, close_mosaic=10, seed=42,
         project=r"C:\Users\93130\Desktop\WaterWasteDetection\runs\detect",
         name="yolo11n_v4_80", exist_ok=True, device=0, workers=0)
