<template>
  <div class="txt-reader">
    <div class="toolbar">
      <el-button :disabled="page <= 1" @click="prev">上一页</el-button>
      <span class="page-info">{{ page }} / {{ totalPages }}</span>
      <el-button :disabled="page >= totalPages" @click="next">下一页</el-button>
      <span class="tip">阅读进度已自动保存</span>
    </div>
    <div class="content" v-if="content" v-html="rendered"></div>
    <el-empty v-else description="文本加载中..." />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getContent, getProgress, saveProgress } from '../api/books'

const props = defineProps({
  bookId: { type: [Number, String], required: true }
})

const PAGE_SIZE = 1800
const content = ref('')
const page = ref(1)

const pages = computed(() => {
  if (!content.value) return []
  const list = []
  for (let i = 0; i < content.value.length; i += PAGE_SIZE) {
    list.push(content.value.slice(i, i + PAGE_SIZE))
  }
  return list
})
const totalPages = computed(() => Math.max(1, pages.value.length))
const rendered = computed(() => escapeHtml(pages.value[page.value - 1] || '').replace(/\n/g, '<br/>'))

watch(page, (p) => {
  if (props.bookId) {
    saveProgress(props.bookId, JSON.stringify({ page: p })).catch(() => {})
  }
})

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function prev() {
  if (page.value > 1) page.value -= 1
}

function next() {
  if (page.value < totalPages.value) page.value += 1
}

async function load() {
  try {
    content.value = await getContent(props.bookId)
    try {
      const progress = await getProgress(props.bookId)
      if (progress?.position) {
        const parsed = JSON.parse(progress.position)
        if (parsed.page) page.value = Number(parsed.page)
      }
    } catch (e) {
      /* 未登录时无进度 */
    }
  } catch (e) {
    ElMessage.error('文本加载失败')
  }
}

load()
</script>

<style scoped>
.txt-reader {
  max-width: 860px;
  margin: 0 auto;
  background: #fffdf7;
  padding: 24px 48px;
  min-height: 70vh;
  border: 1px solid #e4e7ed;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}
.page-info {
  font-size: 14px;
  color: #606266;
}
.tip {
  font-size: 12px;
  color: #c0c4cc;
}
.content {
  font-size: 17px;
  line-height: 2;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
