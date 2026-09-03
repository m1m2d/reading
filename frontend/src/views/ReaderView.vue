<template>
  <div class="reader-page">
    <header class="reader-topbar">
      <el-button text @click="$router.push(`/book/${route.params.id}`)">
        <el-icon><Back /></el-icon> 返回详情
      </el-button>
      <span class="title">{{ book?.title || '在线阅读' }}</span>
      <el-button text @click="download">
        <el-icon><Download /></el-icon> 下载
      </el-button>
    </header>
    <div class="reader-body">
      <TxtReader v-if="book?.fileFormat === 'txt'" :book-id="route.params.id" />
      <PdfReader v-else-if="book?.fileFormat === 'pdf'" :book-id="route.params.id" />
      <div v-else class="no-reader">
        <el-empty description="该格式暂不支持网页阅读，请下载后使用本地阅读器">
          <el-button type="primary" @click="download">下载原文件</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TxtReader from '../components/TxtReader.vue'
import PdfReader from '../components/PdfReader.vue'
import { getBook } from '../api/books'

const route = useRoute()
const book = ref(null)

function download() {
  window.open(`/api/v1/books/${route.params.id}/download`, '_blank')
}

onMounted(async () => {
  book.value = await getBook(route.params.id)
})
</script>

<style scoped>
.reader-page {
  min-height: 100vh;
  background: #eef1f5;
}
.reader-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 0 24px;
  height: 56px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
}
.title {
  font-weight: 600;
  color: #303133;
}
.reader-body {
  padding: 24px;
}
.no-reader {
  padding: 60px 0;
}
</style>
