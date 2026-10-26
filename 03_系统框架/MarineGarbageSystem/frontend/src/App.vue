<template>
  <div class="app">
    <header class="header">
      <div class="brand">
        <div class="logo">🌊</div>
        <div>
          <div class="title">近海漂浮垃圾自动监测与溯源分析系统</div>
          <div class="sub">无人机影像 · YOLO识别 · FVCOM潮流 · GIS溯源</div>
        </div>
      </div>
      <nav class="tabs">
        <button :class="{active: tab==='detect'}" @click="tab='detect'">智能识别</button>
        <button :class="{active: tab==='map'}" @click="tab='map'">地图溯源</button>
      </nav>
    </header>

    <main class="main">
      <DetectView v-if="tab==='detect'" @result="onResult" />
      <TraceMapView v-else :result="currentResult" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import DetectView from './views/DetectView.vue'
import TraceMapView from './views/TraceMapView.vue'

const tab = ref('detect')
const currentResult = ref(null)
function onResult(r) { currentResult.value = r }
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; background: #f4f6f9; }
.app { font-family: 'Microsoft YaHei', sans-serif; color: #1a2330; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 24px; background: #fff; border-bottom: 1px solid #e3e8ef;
  flex-wrap: wrap; gap: 8px;
}
.brand { display: flex; align-items: center; gap: 10px; }
.logo { font-size: 26px; }
.title { font-size: 17px; font-weight: 700; color: #24488c; }
.sub { font-size: 12px; color: #7f8fa6; }
.tabs { display: flex; gap: 6px; }
.tabs button {
  padding: 8px 20px; border: 1px solid #d6dee8; background: #fff; border-radius: 20px;
  cursor: pointer; font-size: 14px; color: #3d4a59;
}
.tabs button.active { background: #24488c; color: #fff; border-color: #24488c; }
.main { max-width: 1100px; margin: 20px auto; padding: 0 16px; }
</style>
