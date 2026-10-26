<template>
  <div class="page">
    <h2 class="page-title">无人机影像智能识别</h2>
    <div class="card">
      <!-- 参数区 -->
      <div class="controls">
        <label class="ctrl">
          <span>模型</span>
          <select v-model="model">
            <option v-for="m in models" :key="m.id" :value="m.id" :disabled="!m.exists">
              {{ m.id }} {{ m.exists ? '' : '(缺失)' }}
            </option>
          </select>
        </label>
        <label class="ctrl">
          <span>置信度阈值</span>
          <input type="number" v-model.number="conf" min="0.05" max="0.95" step="0.05" />
        </label>
        <label class="ctrl">
          <span>选择图片</span>
          <input type="file" accept="image/*" @change="onFile" />
        </label>
        <button class="btn primary" :disabled="!file || loading" @click="run">
          {{ loading ? '分析中…' : '开始识别分析' }}
        </button>
      </div>
      <div v-if="error" class="error">{{ error }}</div>

      <!-- 结果区 -->
      <div v-if="result" class="result-grid">
        <div class="img-box">
          <canvas ref="cv" :width="canvasW" :height="canvasH"
                  style="max-width:100%; border:1px solid #ddd; background:#f5f7fa;"></canvas>
          <div class="img-caption">识别结果（{{ result.model }}）</div>
        </div>
        <div class="info-box">
          <h3>检出目标（{{ result.detection.num_detections }}）</h3>
          <table class="tbl">
            <thead><tr><th>类别</th><th>置信度</th><th>中心</th></tr></thead>
            <tbody>
              <tr v-for="(d,i) in result.detection.detections" :key="i">
                <td>{{ d.class_cn }}({{ d.class }})</td>
                <td>{{ d.confidence.toFixed(3) }}</td>
                <td>[{{ d.center[0] }}, {{ d.center[1] }}]</td>
              </tr>
            </tbody>
          </table>
          <div class="agg" v-if="result.prediction.aggregations.length">
            <h4>潮流聚集预测</h4>
            <p v-for="(a,i) in result.prediction.aggregations" :key="i">
              聚集区{{ i+1 }}：中心 {{ a.center[0] }},{{ a.center[1] }} · 半径 {{ a.radius }} · 粒子 {{ a.count }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getModels, analyzeImage } from '../api'

const emit = defineEmits(['result'])
const models = ref([])
const model = ref('v4b')
const conf = ref(0.25)
const file = ref(null)
const loading = ref(false)
const error = ref('')
const result = ref(null)
const cv = ref(null)
const canvasW = ref(800)
const canvasH = ref(600)

onMounted(async () => {
  try {
    const m = await getModels()
    models.value = m.models
    if (!m.models.some(x => x.id === model.value)) model.value = m.default
  } catch (e) { error.value = '加载模型列表失败: ' + e.message }
})

function onFile(e) {
  const f = e.target.files[0]
  if (f) { file.value = f; error.value = '' }
}

async function run() {
  if (!file.value) return
  loading.value = true; error.value = ''
  try {
    const r = await analyzeImage(file.value, { model: model.value, conf: conf.value })
    result.value = r
    emit('result', r)
    await nextTick()
    drawDetections(r)
  } catch (e) {
    error.value = '分析失败: ' + e.message
  } finally { loading.value = false }
}

function drawDetections(r) {
  const canvas = cv.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const [w, h] = r.detection.image_size
  canvasW.value = w; canvasH.value = h
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    canvas.width = w; canvas.height = h
    ctx.drawImage(img, 0, 0, w, h)
    const colors = { Glass: '#4a90d9', Metal: '#9a9a9a', Plastic: '#4caf50', Trash: '#e1a739' }
    for (const d of r.detection.detections) {
      const [x1, y1, x2, y2] = d.bbox_xyxy
      const c = colors[d.class] || '#333'
      ctx.strokeStyle = c; ctx.lineWidth = 3
      ctx.strokeRect(x1, y1, x2 - x1, y2 - y1)
      ctx.fillStyle = c
      ctx.font = 'bold 18px sans-serif'
      const label = `${d.class_cn} ${d.confidence.toFixed(2)}`
      ctx.fillText(label, x1, y1 > 22 ? y1 - 6 : y1 + 22)
    }
  }
  img.src = r.image
}
</script>

<style scoped>
.page { font-family: 'Microsoft YaHei', sans-serif; }
.page-title { font-size: 18px; color: #24488c; margin: 4px 0 12px; }
.card { border: 1px solid #e3e8ef; border-radius: 10px; padding: 16px; }
.controls { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; margin-bottom: 14px; }
.ctrl { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #3d4a59; }
.ctrl select, .ctrl input { padding: 6px 8px; border: 1px solid #ccc; border-radius: 6px; }
.btn.primary { background: #24488c; color: #fff; border: none; padding: 8px 18px; border-radius: 6px; cursor: pointer; }
.btn.primary:disabled { background: #aab; }
.error { color: #c0392b; margin: 8px 0; }
.result-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; }
.img-box img, .img-box canvas { width: 100%; }
.img-caption { font-size: 12px; color: #5a6b7d; margin-top: 4px; }
.info-box h3 { font-size: 15px; color: #333; }
.tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.tbl th, .tbl td { border: 1px solid #e3e8ef; padding: 5px 8px; text-align: left; }
.tbl th { background: #f0f4fa; }
.agg { margin-top: 12px; font-size: 13px; color: #3d4a59; }
.agg h4 { color: #8a5a10; margin: 8px 0 4px; }
@media (max-width: 900px) { .result-grid { grid-template-columns: 1fr; } }
</style>
