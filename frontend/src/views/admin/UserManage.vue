<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 240px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
    </div>
    <el-table :data="users" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="showActions(row)">行为日志</el-button>
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" text
                     @click="toggleStatus(row)">
            {{ row.status === 1 ? '封禁' : '解封' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :page-size="size"
                     :current-page="page" @current-change="load" />
    </div>

    <el-drawer v-model="drawerVisible" :title="`行为日志 - ${currentUser?.username || ''}`" size="560px">
      <el-table :data="actions" v-loading="actionLoading" size="small" border>
        <el-table-column prop="action" label="动作" width="130" />
        <el-table-column prop="detail" label="详情" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="110" />
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
      <div class="pager">
        <el-pagination layout="prev, pager, next" :total="actionTotal" :page-size="actionSize"
                       :current-page="actionPage" @current-change="loadActions" small />
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, setUserStatus, userActions } from '../../api/admin'

const keyword = ref('')
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const drawerVisible = ref(false)
const currentUser = ref(null)
const actions = ref([])
const actionTotal = ref(0)
const actionPage = ref(1)
const actionSize = 5
const actionLoading = ref(false)

async function load(p) {
  page.value = p || page.value
  loading.value = true
  try {
    const data = await listUsers({ keyword: keyword.value || undefined, page: page.value, size })
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const action = row.status === 1 ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await setUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

async function showActions(row) {
  currentUser.value = row
  drawerVisible.value = true
  await loadActions(1)
}

async function loadActions(p) {
  actionPage.value = p || actionPage.value
  actionLoading.value = true
  try {
    const data = await userActions(currentUser.value.id, { page: actionPage.value, size: actionSize })
    actions.value = data.records
    actionTotal.value = data.total
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
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
