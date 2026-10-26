# -*- coding: utf-8 -*-
"""Pydantic 数据模型（请求/响应结构）"""
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field


class Detection(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    class_id: int
    class_name: str = Field(alias="class")
    class_cn: str
    confidence: float
    bbox_xyxy: List[float]
    center: List[float]
    size: List[float]


class AnalyzeRequest(BaseModel):
    """（预留）JSON 请求体；实际走 multipart 上传图片"""
    model: str = "v4b"
    conf: float = 0.25
    iou: float = 0.45
    track_steps: int = 40
