import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import router from '../router'
import { monitor } from '../utils/monitor'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 120000
})

let traceId = localStorage.getItem('x-trace-id') || Math.random().toString(16).slice(2, 14)
localStorage.setItem('x-trace-id', traceId)

request.interceptors.request.use((config) => {
  config.headers['X-Trace-Id'] = traceId
  const token = localStorage.getItem('cloudread_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  monitor.countApi(true)
  return config
})

let refreshing = null
let backendDown = false
let lastDownNotify = 0
const DOWN_NOTIFY_INTERVAL = 30000

/**
 * 后端服务不可达时弹出提示（节流：30 秒内最多提示一次，恢复后提示已恢复）。
 */
function notifyBackendDown() {
  const now = Date.now()
  if (backendDown) {
    return
  }
  if (now - lastDownNotify < DOWN_NOTIFY_INTERVAL) {
    return
  }
  backendDown = true
  lastDownNotify = now
  ElMessageBox.alert(
    '后端服务似乎已停止，请确认后端已启动（可双击项目根目录的 start.bat 一键启动）。',
    '服务不可用',
    { confirmButtonText: '知道了', type: 'error', closeOnClickModal: false }
  ).catch(() => {})
}

function notifyBackendUp() {
  if (backendDown) {
    backendDown = false
    ElMessageBox.alert('后端服务已恢复，可以继续操作。', '服务已恢复', {
      confirmButtonText: '好的',
      type: 'success',
      closeOnClickModal: false
    }).catch(() => {})
  }
}

request.interceptors.response.use(
  (response) => {
    notifyBackendUp()
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        monitor.countApi(false)
        if (body.code === 401) {
          return handleUnauthorized(response.config)
        }
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return body
  },
  async (error) => {
    monitor.countApi(false)
    const { response } = error
    const status = response?.status
    const hasBusinessBody = response?.data && typeof response.data.code === 'number'
    // 无响应（连接失败/超时）或网关/代理返回的无业务结构的 5xx，视为后端服务不可用
    const isBackendDown = !response || (status >= 500 && !hasBusinessBody)
    if (isBackendDown) {
      notifyBackendDown()
    } else {
      notifyBackendUp()
    }
    if (response && response.status === 401) {
      return handleUnauthorized(error.config)
    }
    const msg = response?.data?.message || error.message || '网络异常'
    if (!error.config?.silent) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

async function handleUnauthorized(config) {
  const authStore = useAuthStore()
  if (!authStore.token || authStore.isRefreshing) {
    authStore.logout()
    router.push('/login')
    return Promise.reject(new Error('登录已过期'))
  }
  if (!refreshing) {
    refreshing = authStore
      .refresh()
      .catch(() => {
        authStore.logout()
        router.push('/login')
      })
      .finally(() => {
        refreshing = null
      })
  }
  await refreshing
  config.headers.Authorization = `Bearer ${authStore.token}`
  return request(config)
}

export default request
