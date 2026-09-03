<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="后端指标" name="backend">
      <el-card>
        <div class="toolbar">
          <el-button type="primary" :loading="refreshing" @click="loadMetrics">刷新</el-button>
          <span class="hint">上次刷新：{{ lastRefresh }}</span>
        </div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="JVM 内存">{{ metrics.jvm?.memoryUsedMb }} / {{ metrics.jvm?.memoryMaxMb }} MB</el-descriptions-item>
          <el-descriptions-item label="已提交内存">{{ metrics.jvm?.memoryCommittedMb }} MB</el-descriptions-item>
          <el-descriptions-item label="进程 CPU">{{ metrics.jvm?.processCpu }}%</el-descriptions-item>
          <el-descriptions-item label="系统 CPU">{{ metrics.jvm?.systemCpu }}%</el-descriptions-item>
          <el-descriptions-item label="活动线程">{{ metrics.jvm?.threads }}</el-descriptions-item>
          <el-descriptions-item label="GC 暂停">{{ metrics.jvm?.gcPauseCount }} 次 / {{ metrics.jvm?.gcPauseTimeMs }} ms</el-descriptions-item>
          <el-descriptions-item label="运行时长">{{ Math.floor((metrics.jvm?.uptimeSeconds || 0) / 60) }} 分钟</el-descriptions-item>
          <el-descriptions-item label="HTTP 总请求">{{ metrics.http?.totalRequests }}</el-descriptions-item>
          <el-descriptions-item label="接口成功率">{{ metrics.http?.successRate }}%</el-descriptions-item>
          <el-descriptions-item label="平均响应">{{ metrics.http?.avgResponseMs }} ms</el-descriptions-item>
          <el-descriptions-item label="最大响应">{{ metrics.http?.maxResponseMs }} ms</el-descriptions-item>
          <el-descriptions-item label="慢查询">{{ metrics.db?.slowQueries }} 条</el-descriptions-item>
          <el-descriptions-item label="SQLite 文件">{{ metrics.db?.dbFileMb }} MB</el-descriptions-item>
          <el-descriptions-item label="WAL 文件">{{ metrics.db?.walFileMb }} MB</el-descriptions-item>
          <el-descriptions-item label="连接池活跃/空闲/总数">
            {{ metrics.db?.activeConnections }} / {{ metrics.db?.idleConnections }} / {{ metrics.db?.totalConnections }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="前端监控" name="frontend">
      <el-card>
        <el-table :data="frontendList" v-loading="frontendLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="pageUrl" label="页面" min-width="140" />
          <el-table-column prop="fcpMs" label="FCP(ms)" width="90" />
          <el-table-column prop="lcpMs" label="LCP(ms)" width="90" />
          <el-table-column prop="jsErrors" label="JS异常" width="80" />
          <el-table-column prop="apiTotal" label="API总数" width="90" />
          <el-table-column label="API成功率" width="110">
            <template #default="{ row }">
              {{ row.apiTotal ? Math.round((row.apiTotal - row.apiFail) / row.apiTotal * 100) : '-' }}%
            </template>
          </el-table-column>
          <el-table-column prop="userAgent" label="浏览器环境" show-overflow-tooltip min-width="200" />
          <el-table-column prop="createdAt" label="上报时间" width="170" />
        </el-table>
        <div class="pager">
          <el-pagination layout="prev, pager, next, total" :total="frontendTotal" :page-size="frontendSize"
                         :current-page="frontendPage" @current-change="loadFrontend" />
        </div>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="实时日志流" name="realtime">
      <el-card>
        <div class="toolbar">
          <el-button :type="connected ? 'danger' : 'success'" @click="toggleWs">
            {{ connected ? '断开连接' : '连接日志流' }}
          </el-button>
          <el-select v-model="wsLevel" placeholder="级别过滤" clearable style="width: 130px">
            <el-option label="ERROR" value="ERROR" />
            <el-option label="WARN" value="WARN" />
          </el-select>
          <el-input v-model="wsKeyword" placeholder="关键字过滤" clearable style="width: 220px" />
          <el-button size="small" @click="clearLogs">清空</el-button>
          <span class="hint">{{ connected ? '实时推送中...' : '未连接' }}</span>
        </div>
        <el-table :data="filteredLogs" size="small" border max-height="520">
          <el-table-column label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="row.level === 'ERROR' ? 'danger' : 'warning'" size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="165" />
          <el-table-column prop="traceId" label="TraceID" width="120" show-overflow-tooltip />
          <el-table-column prop="logger" label="来源" width="220" show-overflow-tooltip />
          <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>
    </el-tab-pane>

    <el-tab-pane label="系统日志" name="syslog">
      <el-card>
        <div class="toolbar">
          <el-select v-model="logLevel" placeholder="级别过滤" clearable style="width: 130px">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
          <el-input v-model="logKeyword" placeholder="关键字过滤" clearable style="width: 220px" @keyup.enter="loadLogs" />
          <el-button type="primary" @click="loadLogs">查询</el-button>
        </div>
        <el-table :data="sysLogs" v-loading="logLoading" size="small" border>
          <el-table-column label="级别" width="80">
            <template #default="{ row }">
              <el-tag :type="row.level === 'ERROR' ? 'danger' : row.level === 'WARN' ? 'warning' : 'info'" size="small">
                {{ row.level }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="traceId" label="TraceID" width="130" show-overflow-tooltip />
          <el-table-column prop="module" label="模块" width="80" />
          <el-table-column prop="method" label="方法" width="70" />
          <el-table-column prop="requestUri" label="请求路径" width="200" show-overflow-tooltip />
          <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
          <el-table-column prop="costMs" label="耗时(ms)" width="90" />
          <el-table-column prop="ip" label="IP" width="120" />
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
        <div class="pager">
          <el-pagination layout="prev, pager, next, total" :total="logTotal" :page-size="logSize"
                         :current-page="logPage" @current-change="loadLogs" />
        </div>
      </el-card>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { backendMetrics, frontendMonitors, systemLogs } from '../../api/admin'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const tab = ref('backend')

// 后端指标
const metrics = ref({})
const refreshing = ref(false)
const lastRefresh = ref('')

async function loadMetrics() {
  refreshing.value = true
  try {
    metrics.value = await backendMetrics()
    lastRefresh.value = new Date().toLocaleTimeString()
  } finally {
    refreshing.value = false
  }
}

// 前端监控
const frontendList = ref([])
const frontendTotal = ref(0)
const frontendPage = ref(1)
const frontendSize = 10
const frontendLoading = ref(false)

async function loadFrontend(page) {
  frontendPage.value = page || frontendPage.value
  frontendLoading.value = true
  try {
    const data = await frontendMonitors({ page: frontendPage.value, size: frontendSize })
    frontendList.value = data.records
    frontendTotal.value = data.total
  } finally {
    frontendLoading.value = false
  }
}

// 实时日志
const connected = ref(false)
const wsLevel = ref('')
const wsKeyword = ref('')
const logs = ref([])
let ws = null

const filteredLogs = computed(() =>
  logs.value.filter((l) => {
    if (wsLevel.value && l.level !== wsLevel.value) return false
    if (wsKeyword.value && !(l.message || '').includes(wsKeyword.value)) return false
    return true
  })
)

function toggleWs() {
  if (connected.value) {
    ws && ws.close()
    connected.value = false
    return
  }
  const proto = location.protocol === 'https:' ? 'wss://' : 'ws://'
  ws = new WebSocket(`${proto}${location.host}/ws/logs?token=${auth.token}`)
  ws.onopen = () => {
    connected.value = true
    ElMessage.success('日志流已连接')
    ws.send(JSON.stringify({ token: auth.token }))
  }
  ws.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data)
      if (msg.type === 'history') {
        logs.value = [...msg.payload.reverse(), ...logs.value].slice(0, 2000)
      } else if (msg.type === 'event') {
        logs.value.unshift(msg.payload)
        if (logs.value.length > 2000) logs.value.pop()
      }
    } catch (err) {
      /* 忽略 */
    }
  }
  ws.onclose = () => {
    connected.value = false
  }
  ws.onerror = () => {
    ElMessage.error('日志流连接失败')
  }
}

function clearLogs() {
  logs.value = []
}

// 系统日志
const sysLogs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const logSize = 10
const logLevel = ref('')
const logKeyword = ref('')
const logLoading = ref(false)

async function loadLogs(page) {
  logPage.value = page || logPage.value
  logLoading.value = true
  try {
    const data = await systemLogs({
      level: logLevel.value || undefined,
      keyword: logKeyword.value || undefined,
      page: logPage.value,
      size: logSize
    })
    sysLogs.value = data.records
    logTotal.value = data.total
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  loadMetrics()
  loadFrontend(1)
  loadLogs(1)
})

onBeforeUnmount(() => ws && ws.close())
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
