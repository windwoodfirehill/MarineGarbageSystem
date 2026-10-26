// 后端 API 封装（通过 vite 代理 /api -> 后端 8000）
const BASE = '/api'

export async function getModels() {
  const r = await fetch(`${BASE}/models`)
  return r.json()
}

export async function getSources() {
  const r = await fetch(`${BASE}/sources`)
  return r.json()
}

export async function analyzeImage(file, { model = 'v4b', conf = 0.25 } = {}) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('model', model)
  fd.append('conf', String(conf))
  const r = await fetch(`${BASE}/analyze`, { method: 'POST', body: fd })
  if (!r.ok) {
    const e = await r.json().catch(() => ({}))
    throw new Error(e.detail || `请求失败 ${r.status}`)
  }
  return r.json()
}
