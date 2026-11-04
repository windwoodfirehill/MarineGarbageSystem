export const typeNames={IMAGE:'图片检测',VIDEO:'录像分析',STREAM:'无人机直播'}
export const classNames={Plastic:'塑料',Glass:'玻璃',Metal:'金属',Trash:'其他漂浮垃圾'}
export const statusNames={COMPLETED:'已完成预测',WAITING_FOR_DATA:'等待历史流场',NO_VALID_WATER:'区域缺少有效流场',NO_DETECTIONS:'未检出垃圾'}
export function dateText(value){if(!value)return '未填写';const d=new Date(value);return Number.isNaN(d.getTime())?String(value):new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false,timeZone:'Asia/Shanghai'}).format(d)}
export function areaText(a){return a?`东经 ${Number(a.west).toFixed(4)}°—${Number(a.east).toFixed(4)}°，北纬 ${Number(a.south).toFixed(4)}°—${Number(a.north).toFixed(4)}°`:'未设置观测位置'}
export function classCounts(det){const out={};for(const d of det?.detections||[])out[d.class_cn||classNames[d.class]||d.class]=(out[d.class_cn||classNames[d.class]||d.class]||0)+1;return Object.entries(out).map(([name,count])=>({name,count})).sort((a,b)=>b.count-a.count)}
