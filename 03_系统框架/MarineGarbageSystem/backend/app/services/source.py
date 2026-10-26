# -*- coding: utf-8 -*-
"""污染源溯源服务（GIS 关联分析）

初版：以模拟污染源点位（河流入口/养殖区/港口）在图片像素空间做关联分析，
通过距离反比加权估算各污染源对识别垃圾的贡献度；后续接入真实 GIS 点位数据。
"""
import math
from .. import config

# 模拟污染源点位（示例，图片像素空间；后续替换为真实 GIS 河流入口/养殖区/港口坐标）
# weight 表示该污染源的基础排放强度
POLLUTION_SOURCES = [
    {"id": "river_east",  "name": "东部河流入海口", "type": "河流入口", "x": 60,   "y": 220,  "weight": 0.9},
    {"id": "river_west",  "name": "西部河流入海口", "type": "河流入口", "x": 540,  "y": 60,   "weight": 0.7},
    {"id": "aqua",        "name": "近岸养殖区",     "type": "养殖区",   "x": 300,  "y": 480,  "weight": 0.5},
    {"id": "port",        "name": "渔港码头",       "type": "港口",     "x": 620,  "y": 380,  "weight": 0.6},
]


def _distance(p1, p2):
    return math.hypot(p1[0] - p2[0], p1[1] - p2[1])


def trace_sources(positions, sources=None, decay=200.0):
    """溯源分析：由垃圾/聚集区位置估算各污染源贡献度

    positions: list of (x, y) 垃圾或聚集中心坐标
    返回: 污染源贡献度排序 + 总体结论
    """
    sources = sources or POLLUTION_SOURCES
    if not positions:
        return {"sources": [], "top_source": None,
                "note": "无目标位置，无法溯源"}

    # 每个污染源对每个垃圾点的贡献 = weight * exp(-dist/decay)
    contributions = {s["id"]: 0.0 for s in sources}
    for (x, y) in positions:
        for s in sources:
            d = _distance((s["x"], s["y"]), (x, y))
            contrib = s["weight"] * math.exp(-d / decay)
            contributions[s["id"]] += contrib

    # 归一化到 0-100
    total = sum(contributions.values()) or 1.0
    ranked = []
    for s in sources:
        score = round(100 * contributions[s["id"]] / total, 1)
        ranked.append({
            "id": s["id"], "name": s["name"], "type": s["type"],
            "position": [s["x"], s["y"]],
            "contribution": score,
        })
    ranked.sort(key=lambda r: r["contribution"], reverse=True)
    top = ranked[0] if ranked else None
    return {
        "sources": ranked,
        "top_source": top,
        "note": "模拟污染源点位示意，待接入真实GIS数据",
    }


def run_trace(detections, aggregations):
    """高层入口：由识别 + 聚集预测结果综合溯源

    detections: detector 输出列表；aggregations: fvcom 聚集区域列表
    """
    # 溯源锚点：优先用聚集区域中心，其次用检测目标中心
    anchors = []
    if aggregations:
        anchors = [tuple(a["center"]) for a in aggregations]
    elif detections:
        anchors = [tuple(d["center"]) for d in detections]
    return trace_sources(anchors)
