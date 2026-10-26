# -*- coding: utf-8 -*-
"""FastAPI 入口"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import config
from .api.routes import router

app = FastAPI(
    title="近海漂浮垃圾自动监测与溯源分析系统",
    description="无人机影像采集 -> AI识别(YOLO) -> 潮流预测(FVCOM粒子追踪) -> 污染源溯源(GIS)",
    version="0.1.0",
)

# CORS：允许前端(Vue dev server)跨域调用
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载静态目录（上传图片、结果图可被前端直接访问）
app.mount("/static", StaticFiles(directory=config.APP_DIR + "/static"), name="static")

# 业务路由
app.include_router(router)


@app.get("/")
def root():
    return {"service": "marine-garbage-api", "status": "running",
            "docs": "/docs"}
