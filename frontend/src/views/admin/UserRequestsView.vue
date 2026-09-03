<template>
  <el-card>
    <template #header>
      <div class="card-head">
        <span>{{ isLog ? '用户请求日志' : '用户请求（待处理）' }}</span>
        <span class="hint">{{ isLog ? '所有已完成（归档）的用户请求' : '忘记密码等请求，处理完成后点击对号归档' }}</span>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名 / 邮箱 / 详情" clearable style="width: 260px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="load()">刷新</el-button>
    </div>
    <el-table :data="requests" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="email" label="电子邮件" width="200" show-overflow-tooltip />
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="提交时间" width="160" />
      <el-table-column v-if="isLog" prop="processedByName" label="处理人" width="110" />
      <el-table-column v-if="isLog" prop="processedAt" label="处理时间" width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '已归档' : '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isLog" label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="resetPassword(row)">重置密码</el-button>
          <el-button size="small" type="success" @click="archive(row)">
            <el-icon><Select /></el-icon> 对号归档
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :page-size="size"
                     :current-page="page" @current-change="load" />
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUserRequests, resetUserPassword, archiveUserRequest } from '../../api/userRequests'

const route = useRoute()
const isLog = computed(() => route.path.includes('user-request-logs'))

const requests = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const loading = ref(false)

async function load(p) {
  page.value = p || page.value
  loading.value = true
  try {
    const data = await listUserRequests({
      status: isLog.value ? 1 : 0,
      keyword: keyword.value || undefined,
      page: page.value,
      size
    })
    requests.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function resetPassword(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为用户「${row.username}」设置新密码：`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPattern: /^.{6,64}$/,
        inputErrorMessage: '密码长度需在 6-64 之间'
      }
    )
    await resetUserPassword(row.id, value)
    ElMessage.success(`已重置 ${row.username} 的密码，点击对号归档`)
    load()
  } catch (e) {
    /* 用户取消 */
  }
}

async function archive(row) {
  await ElMessageBox.confirm(`确定归档用户「${row.username}」的请求吗？`, '归档确认', { type: 'warning' })
  await archiveUserRequest(row.id)
  ElMessage.success('已归档')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  gap: 14px;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
