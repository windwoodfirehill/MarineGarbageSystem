# -*- coding: utf-8 -*-
"""v3 / v4 / v4b 三版本关键指标对比"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
matplotlib.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
matplotlib.rcParams["axes.unicode_minus"] = False

data = {
    "v3":  {"P": 0.852, "R": 0.763, "mAP50": 0.820, "mAP50-95": 0.606},
    "v4":  {"P": 0.8805, "R": 0.7417, "mAP50": 0.8172, "mAP50-95": 0.6087},
    "v4b": {"P": 0.8661, "R": 0.7508, "mAP50": 0.8201, "mAP50-95": 0.6042},
}
labels = ["Precision", "Recall", "mAP50", "mAP50-95"]
colors = {"v3": "#7f8fa6", "v4": "#e1a739", "v4b": "#4a90d9"}
names = ["v3（基线,无负样本）", "v4（负样本888）", "v4b（水面增强+mixup）"]

x = np.arange(len(labels)); w = 0.25
fig, ax = plt.subplots(figsize=(11, 6), dpi=150)
bars = []
for i, (k, nm) in enumerate(zip(data, names)):
    vals = [data[k]["P"], data[k]["R"], data[k]["mAP50"], data[k]["mAP50-95"]]
    b = ax.bar(x + (i-1)*w, vals, w, label=nm, color=colors[k])
    bars.append(b)
    for bb, v in zip(b, vals):
        ax.text(bb.get_x()+bb.get_width()/2, v+0.008, f"{v:.3f}", ha="center", va="bottom", fontsize=9)

ax.set_ylabel("指标值"); ax.set_ylim(0, 1.0)
ax.set_xticks(x); ax.set_xticklabels(labels)
ax.legend(fontsize=10); ax.grid(axis="y", alpha=0.3)
ax.set_title("v3 / v4 / v4b 三版本关键指标对比（best epoch）", fontsize=13)
plt.tight_layout()
out = r"C:\Users\93130\Desktop\WaterWasteDetection\report_v3_work\compare_v3_v4_v4b.png"
plt.savefig(out)
print("已生成:", out)
