<template>
  <div class="page">
    <h2 class="page-title">潮流预测与污染源溯源</h2>
    <div v-if="!result" class="hint">请先在「智能识别」页上传图片完成分析</div>

    <div v-else class="map-layout">
      <div class="map-box">
        <div ref="mapEl" class="map"></div>
      </div>
      <div class="panel">
        <h3>污染源贡献度</h3>
        <div v-if="result.trace.top_source" class="top">
          主要污染源：<b>{{ result.trace.top_source.name }}</b>
          （{{ result.trace.top_source.contribution }}%）
        </div>
        <ul class="src-list">
          <li v-for="s in result.trace.sources" :key="s.id">
            <div class="src-row">
              <span>{{ s.name }}</span>
              <span class="bar"><i :style="{width: s.contribution + '%'}"></i></span>
              <span class="pct">{{ s.contribution }}%</span>
            </div>
          </li>
        </ul>
        <div class="legend">
          <p><b style="color:#c0392b">●</b> 污染源</p>
          <p><b style="color:#4a90d9">○</b> 识别垃圾</p>
          <p><b style="color:#e1a739">◍</b> 聚集区</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import L from 'leaflet'

const props = defineProps({ result: Object })
const mapEl = ref(null)
let map = null
let layerGroup = null

// 把图片像素坐标映射到 Leaflet 平面（0~100 虚拟单位，示意）
function px2map(px, imgW, imgH) {
  const scale = 100 / Math.max(imgW, imgH)
  return { x: px[0] * scale, y: 100 - px[1] * scale } // y 翻转
}

onMounted(() => init())

watch(() => props.result, () => { if (props.result) nextTick(() => draw()) })

function init() {
  if (!mapEl.value || map) return
  map = L.map(mapEl.value).setView([50, 50], 0)
  // 简化底图：不用在线瓦片，用网格示意海域
  L.gridLayer({ tileSize: 25 }).addTo(map)
  layerGroup = L.layerGroup().addTo(map)
  if (props.result) draw()
}

function draw() {
  if (!map || !layerGroup || !props.result) return
  layerGroup.clearLayers()
  const r = props.result
  const [imgW, imgH] = r.detection.image_size

  // 污染源点位
  for (const s of r.trace.sources) {
    const m = px2map(s.position, imgW, imgH)
    L.circleMarker([m.y, m.x], {
      radius: 8, color: '#c0392b', fillColor: '#c0392b', fillOpacity: 0.8,
    }).addTo(layerGroup).bindPopup(`<b>${s.name}</b><br>贡献度 ${s.contribution}%`)
  }

  // 识别垃圾
  for (const d of r.detection.detections) {
    const m = px2map(d.center, imgW, imgH)
    L.circleMarker([m.y, m.x], {
      radius: 5, color: '#4a90d9', fillColor: '#4a90d9', fillOpacity: 0.7,
    }).addTo(layerGroup).bindPopup(`${d.class_cn} ${d.confidence.toFixed(2)}`)
  }

  // 潮流迁移轨迹
  for (const tr of r.prediction.tracks) {
    const latlngs = tr.map(p => { const m = px2map(p, imgW, imgH); return [m.y, m.x] })
    L.polyline(latlngs, { color: '#7f8fa6', weight: 2, dashArray: '5,5' }).addTo(layerGroup)
  }

  // 聚集区域
  for (const a of r.prediction.aggregations) {
    const m = px2map(a.center, imgW, imgH)
    L.circle([m.y, m.x], {
      radius: a.radius * (100 / Math.max(imgW, imgH)),
      color: '#e1a739', fillColor: '#e1a739', fillOpacity: 0.25,
    }).addTo(layerGroup).bindPopup(`聚集区 · 粒子 ${a.count}`)
  }

  // 自适应视野
  const pts = [
    ...r.trace.sources.map(s => { const m = px2map(s.position, imgW, imgH); return [m.y, m.x] }),
    ...r.detection.detections.map(d => { const m = px2map(d.center, imgW, imgH); return [m.y, m.x] }),
  ]
  if (pts.length) map.fitBounds(L.latLngBounds(pts).pad(0.2))
}
</script>

<style scoped>
.page { font-family: 'Microsoft YaHei', sans-serif; }
.page-title { font-size: 18px; color: #24488c; margin: 4px 0 12px; }
.hint { color: #5a6b7d; padding: 40px; text-align: center; border: 1px dashed #ccc; border-radius: 10px; }
.map-layout { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; }
.map-box { border: 1px solid #e3e8ef; border-radius: 10px; overflow: hidden; }
.map { width: 100%; height: 520px; }
.panel { border: 1px solid #e3e8ef; border-radius: 10px; padding: 14px; }
.panel h3 { font-size: 15px; color: #333; margin-top: 0; }
.top { background: #fdf6ec; border: 1px solid #f0dcb8; border-radius: 8px; padding: 10px; font-size: 14px; margin-bottom: 10px; }
.src-list { list-style: none; padding: 0; margin: 0; }
.src-row { display: flex; align-items: center; gap: 8px; font-size: 13px; padding: 4px 0; }
.bar { flex: 1; height: 10px; background: #eee; border-radius: 5px; overflow: hidden; }
.bar i { display: block; height: 100%; background: #e1a739; }
.pct { width: 52px; text-align: right; color: #5a6b7d; }
.legend { margin-top: 12px; font-size: 12px; color: #3d4a59; }
.legend p { margin: 3px 0; }
@media (max-width: 900px) { .map-layout { grid-template-columns: 1fr; } .map { height: 380px; } }
</style>
