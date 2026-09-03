<template>
  <div class="pdf-reader">
    <div class="toolbar">
      <el-button :disabled="page <= 1" @click="prev">上一页</el-button>
      <span>{{ page }} / {{ totalPages }}</span>
      <el-button :disabled="page >= totalPages" @click="next">下一页</el-button>
      <el-input-number v-model="zoom" :min="0.5" :max="2.5" :step="0.25" size="small" />
      <el-button size="small" @click="zoom = 1">100%</el-button>
      <span class="tip">阅读进度已自动保存</span>
    </div>
    <div class="canvas-wrap" :style="{ width: `${pdfWidth}px` }">
      <canvas ref="canvas"></canvas>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as pdfjsLib from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { getProgress, saveProgress } from '../api/books'

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl

const props = defineProps({
  bookId: { type: [Number, String], required: true }
})

const canvas = ref(null)
const page = ref(1)
const totalPages = ref(1)
const zoom = ref(1)
const pdfWidth = ref(800)
let pdfDoc = null
let renderTask = null
let rendering = false
let pendingRender = null

async function renderPage() {
  if (!pdfDoc || !canvas.value) return
  if (rendering) {
    pendingRender = true
    return
  }
  rendering = true
  try {
    if (renderTask) {
      renderTask.cancel()
    }
    const pdfPage = await pdfDoc.getPage(page.value)
    const viewport = pdfPage.getViewport({ scale: zoom.value })
    const ctx = canvas.value.getContext('2d')
    canvas.value.width = viewport.width
    canvas.value.height = viewport.height
    pdfWidth.value = viewport.width
    renderTask = pdfPage.render({ canvasContext: ctx, viewport })
    await renderTask.promise
  } catch (e) {
    /* 渲染取消属于正常情况 */
  } finally {
    rendering = false
    if (pendingRender) {
      pendingRender = false
      renderPage()
    }
  }
}

function prev() {
  if (page.value > 1) page.value -= 1
}

function next() {
  if (page.value < totalPages.value) page.value += 1
}

async function load() {
  try {
    pdfDoc = await pdfjsLib.getDocument(`/api/v1/books/${props.bookId}/file`).promise
    totalPages.value = pdfDoc.numPages
    try {
      const progress = await getProgress(props.bookId)
      if (progress?.position) {
        const parsed = JSON.parse(progress.position)
        if (parsed.page) page.value = Number(parsed.page)
      }
    } catch (e) {
      /* 未登录无进度 */
    }
    await renderPage()
  } catch (e) {
    ElMessage.error('PDF 加载失败')
  }
}

watch(page, (p) => {
  renderPage()
  if (props.bookId) {
    saveProgress(props.bookId, JSON.stringify({ page: p })).catch(() => {})
  }
})

watch(zoom, () => renderPage())

onMounted(load)
onBeforeUnmount(() => {
  if (renderTask) renderTask.cancel()
})
</script>

<style scoped>
.pdf-reader {
  max-width: 1000px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #fff;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-bottom: none;
  margin-bottom: 0;
}
.tip {
  font-size: 12px;
  color: #c0c4cc;
}
.canvas-wrap {
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top: none;
  min-height: 300px;
  margin-bottom: 12px;
}
canvas {
  display: block;
  width: 100%;
}
</style>
