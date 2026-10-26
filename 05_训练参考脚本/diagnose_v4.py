# -*- coding: utf-8 -*-
"""诊断 v4 各类别性能 + Data_v4 类别分布，为阶段B增强定权重"""
import os, glob, csv
from collections import Counter
from ultralytics import YOLO

V4 = r"C:\Users\93130\Desktop\datasets\Data_v4"
BEST = r"C:\Users\93130\Desktop\WaterWasteDetection\runs\detect\yolo11n_v4_80\weights\best.pt"
names = ["Glass", "Metal", "Plastic", "Trash"]

# ---- Data_v4 train 类别分布 ----
cnt = Counter()
total_img = 0
for split in ["train", "valid"]:
    for l in glob.glob(os.path.join(V4, split, "labels", "*.txt")):
        total_img += 1
        try:
            with open(l) as f:
                for line in f:
                    p = line.strip().split()
                    if len(p) == 5:
                        cnt[int(float(p[0]))] += 1
        except Exception:
            pass
print("=== Data_v4 类别框数分布 ===")
for c in range(4):
    print(f"  {names[c]}: {cnt.get(c,0)} 框")
print("  (含负样本: 空标签不计框)")

# ---- v4 best.pt per-class val ----
print("\n=== v4 best.pt per-class 性能 (valid) ===")
m = YOLO(BEST)
res = m.val(data=os.path.join(V4, "data_v4.yaml"), imgsz=640, batch=16, device=0, split="val", verbose=False, workers=0)
print(f"  总: P={res.box.mp:.4f} R={res.box.mr:.4f} mAP50={res.box.map50:.4f} mAP50-95={res.box.map:.4f}")
for i, name in enumerate(names):
    p = res.box.p[i] if i < len(res.box.p) else float("nan")
    r = res.box.r[i] if i < len(res.box.r) else float("nan")
    ap50 = res.box.ap50[i] if i < len(res.box.ap50) else float("nan")
    ap = res.box.ap[i] if i < len(res.box.ap) else float("nan")
    print(f"  {name:>7}: P={p:.4f} R={r:.4f} mAP50={ap50:.4f} mAP50-95={ap:.4f}")

# 保存供参考
perclass = {}
for i, name in enumerate(names):
    perclass[name] = {
        "P": round(float(res.box.p[i]), 4) if i < len(res.box.p) else None,
        "R": round(float(res.box.r[i]), 4) if i < len(res.box.r) else None,
        "mAP50": round(float(res.box.ap50[i]), 4) if i < len(res.box.ap50) else None,
        "mAP50-95": round(float(res.box.ap[i]), 4) if i < len(res.box.ap) else None,
    }
out = {"train_cls": {names[c]: cnt.get(c, 0) for c in range(4)},
       "perclass": perclass,
       "overall": {"P": round(float(res.box.mp), 4), "R": round(float(res.box.mr), 4),
                   "mAP50": round(float(res.box.map50), 4), "mAP50-95": round(float(res.box.map), 4)}}
with open(r"C:\Users\93130\Desktop\WaterWasteDetection\v4_diag.json", "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print("\n诊断结果已存 v4_diag.json")
