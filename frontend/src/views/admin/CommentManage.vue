<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索评论内容" clearable style="width: 240px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
    </div>
    <el-table :data="comments" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="bookId" label="书籍ID" width="80" />
      <el-table-column prop="nickname" label="用户" width="120" />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column label="回复" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.parentId" size="small" type="info">回复</el-tag>
          <span v-else>主评</span>
        </template>
      </el-table-column>
      <el-table-column prop="likeCount" label="点赞" width="70" />
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
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
import { listComments, adminDeleteComment } from '../../api/admin'

const keyword = ref('')
const comments = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

async function load(p) {
  page.value = p || page.value
  loading.value = true
  try {
    const data = await listComments({
      keyword: keyword.value || undefined,
      page: page.value,
      size
    })
    comments.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该评论吗？', '提示', { type: 'warning' })
  await adminDeleteComment(row.id)
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
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
