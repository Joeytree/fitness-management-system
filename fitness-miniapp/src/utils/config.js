// 全局配置
// 注意：8080 端口可能被 Jenkins 等程序占用，后端已改用 8081
const BASE_URL = 'http://localhost:8081/api'
// 后端静态资源根地址（/upload/** 由后端 WebConfig 映射到 upload 目录）
const SERVER_URL = 'http://localhost:8081'
// AI 私教 WebSocket 地址（真机调试需把 localhost 改为电脑局域网 IP）
const WS_URL = 'ws://localhost:8081/ws/ai/chat'

// 补全后端返回的相对文件路径（如 /upload/xxx.gif）
export function fullUrl(url) {
  if (!url) return url
  if (/^https?:\/\//.test(url)) return url
  return SERVER_URL + url
}

export default {
  BASE_URL,
  SERVER_URL,
  WS_URL
}
