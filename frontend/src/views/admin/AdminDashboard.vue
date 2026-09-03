<template>
  <div class="dashboard">
    <div class="cards" v-loading="loading">
      <el-card class="stat-card">
        <div class="stat-value">{{ metrics.jvm?.memoryUsedMb ?? '-' }} <small>MB</small></div>
        <div class="stat-label">JVM 已用内存</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ metrics.jvm?.processCpu ?? '-' }}<small>%</small></div>
        <div class="stat-label">进程 CPU</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ metrics.jvm?.threads ?? '-' }}</div>
        <div class="stat-label">活动线程</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ metrics.db?.dbFileMb ?? '-' }} <small>MB</small></div>
        <div class="stat-label">SQLite 文件</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ metrics.db?.walFileMb ?? '-' }} <small>MB</small></div>
        <div class="stat-label">WAL 文件</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ metrics.http?.successRate ?? '-' }}<small>%</small></div>
        <div class="stat-label">接口成功率</div>
      </el-card>
    </div>

    <el-card class="chart-card">
      <template #header>资源趋势（近 5 分钟，每 5 秒采样）</template>
      <EChart :option="trendOption" height="320px" />
    </el-card>

    <div class="charts-row">
      <el-card class="chart-card">
        <template #header>数据库连接池</template>
        <EChart :option="poolOption" height="280px" />
      </el-card>
      <el-card class="chart-card">
        <template #header>HTTP 接口质量</template>
        <EChart :option="httpOption" height="280px" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import EChart from '../../components/EChart.vue'
import { backendMetrics } from '../../api/admin'

const metrics = ref({})
const loading = ref(false)
const samples = ref([])
let timer = null

async function poll() {
  loading.value = true
  try {
    const data = await backendMetrics()
    metrics.value = data
    samples.value.push({
      time: new Date().toLocaleTimeString(),
      mem: data.jvm?.memoryUsedMb || 0,
      cpu: data.jvm?.processCpu || 0,
      success: data.http?.successRate || 100,
      active: data.db?.activeConnections || 0
    })
    if (samples.value.length > 60) samples.value.shift()
  } finally {
    loading.value = false
  }
}

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['内存(MB)', 'CPU(%)'] },
  grid: { left: 50, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: samples.value.map((s) => s.time) },
  yAxis: { type: 'value' },
  series: [
    { name: '内存(MB)', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: samples.value.map((s) => s.mem) },
    { name: 'CPU(%)', type: 'line', smooth: true, data: samples.value.map((s) => s.cpu) }
  ]
}))

const poolOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['活跃连接'] },
  grid: { left: 50, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: samples.value.map((s) => s.time) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{ name: '活跃连接', type: 'bar', data: samples.value.map((s) => s.active), itemStyle: { color: '#67c23a' } }]
}))

const httpOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: samples.value.map((s) => s.time) },
  yAxis: { type: 'value', min: 0, max: 100 },
  series: [{
    name: '接口成功率(%)',
    type: 'line',
    smooth: true,
    data: samples.value.map((s) => s.success),
    itemStyle: { color: '#409eff' }
  }]
}))

onMounted(() => {
  poll()
  timer = setInterval(poll, 5000)
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
}
.stat-card {
  border-radius: 10px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}
.stat-value small {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 6px;
}
.chart-card {
  margin-top: 16px;
  border-radius: 10px;
}
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 900px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
