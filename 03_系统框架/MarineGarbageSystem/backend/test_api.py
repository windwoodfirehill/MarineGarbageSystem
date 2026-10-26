# -*- coding: utf-8 -*-
"""后端 API 测试：/api/models + /api/analyze 全流程"""
import urllib.request, json, os, uuid

BASE = "http://127.0.0.1:8000"
TEST_IMG = os.path.join(
    os.path.dirname(__file__),
    "static",
    "uploads",
    "60c090356c0d4091b35858f11627ed99.jpg",
)


def get(url):
    return json.loads(urllib.request.urlopen(url, timeout=60).read().decode())


def multipart_post(url, file_path, fields):
    boundary = "----test" + uuid.uuid4().hex
    body = b""
    fn = os.path.basename(file_path)
    body += ("--" + boundary + "\r\n").encode()
    body += ('Content-Disposition: form-data; name="file"; filename="%s"\r\n' % fn).encode()
    body += b"Content-Type: image/jpeg\r\n\r\n"
    with open(file_path, "rb") as f:
        body += f.read()
    body += b"\r\n"
    for k, v in fields.items():
        body += ("--" + boundary + "\r\n").encode()
        body += ('Content-Disposition: form-data; name="%s"\r\n\r\n%s\r\n' % (k, v)).encode()
    body += ("--" + boundary + "--\r\n").encode()
    req = urllib.request.Request(url, data=body,
        headers={"Content-Type": "multipart/form-data; boundary=" + boundary})
    return json.loads(urllib.request.urlopen(req, timeout=120).read().decode())


print("=== 1) GET /api/models ===")
try:
    m = get(BASE + "/api/models")
    print("默认模型:", m["default"])
    print("类别:", m["classes"])
    for x in m["models"]:
        print("  ", x["id"], "存在" if x["exists"] else "缺失", x["weight"])
except Exception as e:
    print("FAIL:", e)

print("\n=== 2) GET /api/sources ===")
try:
    s = get(BASE + "/api/sources")
    for x in s["sources"]:
        print("  ", x["name"], x["type"])
except Exception as e:
    print("FAIL:", e)

print("\n=== 3) POST /api/analyze（识别->预测->溯源 全流程）===")
try:
    r = multipart_post(BASE + "/api/analyze", TEST_IMG, {"model": "v4b", "conf": "0.25"})
    print("识别模型:", r["model"], "| 检出数:", r["detection"]["num_detections"])
    for d in r["detection"]["detections"][:5]:
        print(f"   {d['class']}({d['class_cn']}) conf={d['confidence']} center={d['center']}")
    print("潮流预测: 聚集区数 =", len(r["prediction"]["aggregations"]),
          "| 轨迹点数 =", len(r["prediction"]["tracks"]))
    for a in r["prediction"]["aggregations"][:3]:
        print(f"   聚集中心 {a['center']} 半径 {a['radius']} 粒子 {a['count']}")
    print("溯源:")
    for src in r["trace"]["sources"]:
        print(f"   {src['name']} 贡献度 {src['contribution']}%")
    print("主要污染源:", r["trace"]["top_source"]["name"] if r["trace"]["top_source"] else "无")
    print("图片URL:", r["image"])
except Exception as e:
    import traceback; traceback.print_exc()
    print("FAIL:", e)
