# -*- coding: utf-8 -*-
"""FVCOM 潮流预测服务（粒子追踪）

初版：以模拟潮流场跑通「识别 -> 粒子追踪 -> 聚集区域预测」接口；
后续可替换为读取真实 FVCOM 输出的潮流场数据（u/v 网格 + 时间序列）。
坐标空间：与识别结果一致（图片像素空间），漂移以像素为单位，示意性模拟。
"""
import numpy as np


class SimulatedTideField:
    """模拟潮流场（空间变化 + 随时间缓慢漂移的向量场，示意数据）"""

    def __init__(self, seed=42):
        self.rng = np.random.default_rng(seed)
        # 一个基准流速方向，模拟主流向
        self.base_u, self.base_v = 0.6, -0.3

    def velocity(self, x, y, t):
        """返回 (x,y) 处 t 时刻流速 (u,v)，像素/步"""
        # 空间变化：涡旋式 + 缓变
        u = self.base_u + 0.5 * np.sin(0.02 * y + 0.1 * t) + 0.2 * np.sin(0.01 * x)
        v = self.base_v + 0.4 * np.cos(0.02 * x + 0.1 * t) - 0.2 * np.cos(0.01 * y)
        return u, v


def particle_track(seeds, width, height, steps=40, dt=1.0, seed=42):
    """拉格朗日粒子追踪：从每个种子点沿潮流场积分迁移路径

    seeds: list of (x, y) 像素坐标
    返回: {"tracks": [...], "final_positions": [...]}
    """
    field = SimulatedTideField(seed)
    tracks = []
    final_positions = []
    for (x, y) in seeds:
        path = [(float(x), float(y))]
        cx, cy = float(x), float(y)
        for step in range(steps):
            u, v = field.velocity(cx, cy, step)
            cx += u * dt
            cy += v * dt
            # 边界约束（漂移到图外则停留边缘，示意）
            cx = min(max(cx, 0), width)
            cy = min(max(cy, 0), height)
            path.append((round(cx, 1), round(cy, 1)))
        tracks.append(path)
        final_positions.append(path[-1])
    return {"tracks": tracks, "final_positions": final_positions}


def predict_aggregation(final_positions, width, height, grid=16, min_points=2):
    """聚集区域预测：对粒子终点做网格密度聚类

    返回: 聚集区域列表（中心点、范围半径、粒子数）
    """
    if not final_positions:
        return []
    gx = np.arange(0, width + 1, grid)
    gy = np.arange(0, height + 1, grid)
    # 统计每个网格内粒子数
    regions = {}
    for (x, y) in final_positions:
        ix = int(x // grid)
        iy = int(y // grid)
        key = (ix, iy)
        regions.setdefault(key, []).append((x, y))
    clusters = []
    for (ix, iy), pts in regions.items():
        if len(pts) >= min_points:
            xs = [p[0] for p in pts]
            ys = [p[1] for p in pts]
            cx = sum(xs) / len(xs)
            cy = sum(ys) / len(ys)
            radius = max(max(xs) - min(xs), max(ys) - min(ys)) / 2 + grid
            clusters.append({
                "center": [round(cx, 1), round(cy, 1)],
                "radius": round(float(radius), 1),
                "count": len(pts),
                "density": round(len(pts) / (grid * grid), 4),
            })
    clusters.sort(key=lambda c: c["count"], reverse=True)
    return clusters


def run_particle_simulation(detections, width, height, steps=40):
    """高层入口：由识别结果生成粒子追踪与聚集预测

    detections: detector.detect() 返回的 detections 列表
    """
    if not detections:
        return {"tracks": [], "final_positions": [], "aggregations": [],
                "note": "无检测目标，未执行潮流模拟"}
    seeds = [tuple(d["center"]) for d in detections]
    pt = particle_track(seeds, width, height, steps=steps)
    agg = predict_aggregation(pt["final_positions"], width, height)
    return {
        "tracks": pt["tracks"],
        "final_positions": pt["final_positions"],
        "aggregations": agg,
        "note": "模拟潮流场示意数据，待接入真实FVCOM网格",
    }
