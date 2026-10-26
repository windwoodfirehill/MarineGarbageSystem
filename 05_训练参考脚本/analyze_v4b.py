# -*- coding: utf-8 -*-
import csv, json

CSV = r"C:\Users\93130\Desktop\WaterWasteDetection\runs\detect\yolo11n_v4b\results.csv"
rows = list(csv.DictReader(open(CSV, encoding="utf-8-sig")))
def g(r,k):
    try: return float(r[k])
    except: return float("nan")

print("=== 每10个epoch ===")
for r in rows:
    ep=int(g(r,"epoch"))
    if ep in (5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80):
        print(f"ep{ep:>3} P={g(r,'metrics/precision(B)'):.4f} R={g(r,'metrics/recall(B)'):.4f} mAP50={g(r,'metrics/mAP50(B)'):.4f} mAP50-95={g(r,'metrics/mAP50-95(B)'):.4f}")

best=max(rows,key=lambda r:g(r,"metrics/mAP50(B)"))
print("\n=== best epoch (by mAP50) ===")
print(f"ep={int(g(best,'epoch'))} P={g(best,'metrics/precision(B)'):.4f} R={g(best,'metrics/recall(B)'):.4f} mAP50={g(best,'metrics/mAP50(B)'):.4f} mAP50-95={g(best,'metrics/mAP50-95(B)'):.4f}")

best95=max(rows,key=lambda r:g(r,"metrics/mAP50-95(B)"))
print(f"best-mAP50-95 ep={int(g(best95,'epoch'))} mAP50-95={g(best95,'metrics/mAP50-95(B)'):.4f}")

out={"best_epoch":int(g(best,"epoch")),
     "P":round(g(best,'metrics/precision(B)'),4),"R":round(g(best,'metrics/recall(B)'),4),
     "mAP50":round(g(best,'metrics/mAP50(B)'),4),"mAP50-95":round(g(best,'metrics/mAP50-95(B)'),4)}
json.dump(out,open(r"C:\Users\93130\Desktop\WaterWasteDetection\v4b_best_metrics.json","w"),indent=2)
print("已存 v4b_best_metrics.json:", out)
