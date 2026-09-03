import request from '../api/request'

let fcp = 0
let lcp = 0
let jsErrors = 0
let apiTotal = 0
let apiFail = 0

function observePerf() {
  try {
    const po = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'paint' && entry.name === 'first-contentful-paint') {
          fcp = Math.round(entry.startTime)
        }
        if (entry.entryType === 'largest-contentful-paint') {
          lcp = Math.round(entry.startTime)
        }
      }
    })
    po.observe({ type: 'paint', buffered: true })
    po.observe({ type: 'largest-contentful-paint', buffered: true })
  } catch (e) {
    /* 浏览器不支持则跳过 */
  }
  window.addEventListener('error', () => { jsErrors += 1 })
  window.addEventListener('unhandledrejection', () => { jsErrors += 1 })
}

export const monitor = {
  countApi(success) {
    apiTotal += 1
    if (!success) apiFail += 1
  },
  async report() {
    try {
      const token = localStorage.getItem('cloudread_token')
      await request.post('/monitor/frontend/report', {
        pageUrl: location.pathname,
        fcpMs: fcp,
        lcpMs: lcp,
        jsErrors,
        apiTotal,
        apiFail,
        userAgent: navigator.userAgent
      }, { headers: token ? { Authorization: `Bearer ${token}` } : {}, silent: true })
      // 上报后清零增量
      jsErrors = 0
      apiTotal = 0
      apiFail = 0
    } catch (e) {
      /* 上报失败静默 */
    }
  }
}

observePerf()
setInterval(() => monitor.report(), 30000)
window.addEventListener('pagehide', () => monitor.report())
