<template>
  <el-card>
    <div class="toolbar">
      <el-radio-group v-model="status" @change="onStatusChange">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" placeholder="搜索书名/作者/ISBN" clearable style="width: 240px"
                @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <el-table :data="books" v-loading="loading" border stripe>
      <el-table-column label="封面" width="70">
        <template #default="{ row }">
          <el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" style="width: 42px; height: 56px"
                    :preview-src-list="[row.coverUrl]" preview-teleported />
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="110" show-overflow-tooltip />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column label="格式" width="70">
        <template #default="{ row }">{{ (row.fileFormat || '').toUpperCase() }}</template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传者" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
            {{ row.status === 1 ? '已通过' : row.status === 2 ? '已驳回' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="downloadCount" label="下载" width="70" />
      <el-table-column prop="createdAt" label="上传时间" width="160" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" v-if="row.status !== 1" @click="review(row, 1)">通过</el-button>
          <el-button size="small" type="warning" v-if="row.status !== 2" @click="review(row, 2)">驳回</el-button>
          <el-button size="small" text @click="$router.push(`/book/${row.id}`)">详情</el-button>
          <el-button size="small" type="danger" text @click="remove(row)">删除</el-button>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminBooks, reviewBook, deleteBook } from '../../api/admin'

const status = ref(null)
const keyword = ref('')
const books = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

async function load(p) {
  page.value = p || page.value
  loading.value = true
  try {
    const data = await adminBooks({
      status: status.value ?? undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size
    })
    books.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onStatusChange() {
  load(1)
}

function search() {
  load(1)
}

async function review(row, s) {
  let reason = ''
  if (s === 2) {
    try {
      reason = await ElMessageBox.prompt('请输入驳回原因：', '驳回书籍', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => value)
    } catch (e) {
      return
    }
  }
  await reviewBook(row.id, s, reason)
  ElMessage.success(s === 1 ? '已通过审核' : '已驳回')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除《${row.title}》吗？该操作会同时删除物理文件。`, '危险操作', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteBook(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
