<template>
  <div class="upload-page">
    <div class="body">
      <el-card class="form-card">
        <template #header>上传电子书</template>
        <el-form :model="form" :rules="rules" ref="formRef" label-width="78px" class="upload-form">
          <div class="form-row">
            <el-form-item label="书名" prop="title" class="row-2">
              <el-input v-model="form.title" placeholder="请输入书名" maxlength="128" />
            </el-form-item>
            <el-form-item label="作者" class="row-1">
              <el-input v-model="form.author" placeholder="作者（可选）" maxlength="64" />
            </el-form-item>
          </div>
          <div class="form-row">
            <el-form-item label="ISBN" class="row-1">
              <el-input v-model="form.isbn" placeholder="ISBN（可选）" maxlength="32" />
            </el-form-item>
            <el-form-item label="分类" class="row-1">
              <el-cascader v-model="form.categoryId" :options="categoryOptions"
                           :props="{ checkStrictly: true }" placeholder="选择分类（可选）" clearable style="width: 100%" />
            </el-form-item>
          </div>
          <el-form-item label="简介" class="row-full">
            <el-input v-model="form.description" type="textarea" :rows="2" maxlength="2000"
                      show-word-limit placeholder="书籍简介（可选）" />
          </el-form-item>
          <div class="form-row upload-row">
            <el-form-item label="文件" prop="file" class="row-2 file-item">
              <el-upload drag :auto-upload="false" :limit="1" accept=".pdf,.epub,.txt,.mobi"
                         :on-change="onFileChange" :on-remove="() => (form.file = null)">
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 PDF / EPUB / TXT / MOBI，超过 100MB 自动分片上传
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item label="封面" class="row-1 cover-item">
              <div class="cover-box">
                <el-upload :auto-upload="false" :limit="1" accept=".jpg,.jpeg,.png"
                           :show-file-list="false" :on-change="onCoverChange">
                  <div class="cover-preview">
                    <img v-if="coverPreview" :src="coverPreview" alt="封面预览" />
                    <el-icon v-else class="cover-icon"><Picture /></el-icon>
                    <span class="cover-tip">{{ coverPreview ? '点击更换' : '选择 JPG/PNG 封面' }}</span>
                  </div>
                </el-upload>
                <div class="cover-hint">不选择时自动解析内置封面或生成默认封面</div>
              </div>
            </el-form-item>
          </div>
          <div class="form-row action-row">
            <el-button type="primary" size="large" :loading="uploading" @click="submit">
              {{ uploading ? `上传中 ${progress}%` : '开始上传' }}
            </el-button>
            <el-button size="large" @click="$router.push('/')">取消</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadBookFile, commitBook } from '../api/books'
import { uploadWithChunks } from '../utils/file'
import { getCategories } from '../api/admin'

const router = useRouter()
const formRef = ref()
const uploading = ref(false)
const progress = ref(0)
const coverPreview = ref('')
const coverFile = ref(null)
const categories = ref([])
const CHUNK_THRESHOLD = 100 * 1024 * 1024

const form = reactive({
  title: '',
  author: '',
  isbn: '',
  categoryId: null,
  description: '',
  file: null
})

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  file: [{ required: true, message: '请选择电子书文件', trigger: 'change' }]
}

const categoryOptions = computed(() => mapTree(categories.value))

function mapTree(nodes) {
  return (nodes || []).map((n) => ({
    value: n.id,
    label: n.name,
    children: n.children?.length ? mapTree(n.children) : undefined
  }))
}

function onFileChange(file) {
  form.file = file.raw
}

function onCoverChange(file) {
  const raw = file.raw
  if (!/\.(jpg|jpeg|png)$/i.test(raw.name)) {
    ElMessage.warning('封面仅支持 JPG/PNG 格式')
    return
  }
  coverFile.value = raw
  if (coverPreview.value) URL.revokeObjectURL(coverPreview.value)
  coverPreview.value = URL.createObjectURL(raw)
}

async function submit() {
  await formRef.value.validate()
  uploading.value = true
  progress.value = 0
  try {
    let fileRef
    if (form.file.size > CHUNK_THRESHOLD) {
      fileRef = await uploadWithChunks(form.file, (p) => (progress.value = p))
    } else {
      fileRef = await uploadBookFile(form.file, (p) => (progress.value = p))
    }

    const meta = {
      title: form.title.trim(),
      author: form.author.trim() || undefined,
      isbn: form.isbn.trim() || undefined,
      description: form.description.trim() || undefined,
      categoryId: Array.isArray(form.categoryId) ? form.categoryId.at(-1) : form.categoryId,
      versionOf: undefined
    }

    if (fileRef.duplicate) {
      try {
        await ElMessageBox.confirm(
          `该文件已存在（《${fileRef.existingTitle}》），是否作为新版本上传？`,
          '检测到重复文件',
          { confirmButtonText: '作为新版本上传', cancelButtonText: '放弃上传', type: 'warning' }
        )
        meta.versionOf = fileRef.existingBookId
        meta.changeLog = await ElMessageBox.prompt('请输入本次版本变更说明：', '版本说明', {
          inputValue: '内容更新',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        }).then(({ value }) => value).catch(() => '内容更新')
      } catch (e) {
        if (e === 'cancel' || e === 'close') {
          ElMessage.info('已放弃上传')
          return
        }
        throw e
      }
    }

    const book = await commitBook({ meta, fileRef }, coverFile.value)
    ElMessage.success('上传成功，等待审核后即可上架')
    router.push(`/book/${book.id}`)
  } catch (e) {
    if (e?.message) ElMessage.error(e.message)
  } finally {
    uploading.value = false
  }
}

onMounted(async () => {
  categories.value = await getCategories()
})
</script>

<style scoped>
.upload-page {
  min-height: 100vh;
}
.body {
  max-width: 1040px;
  margin: 20px auto;
  padding: 0 8px;
}
.form-card {
  border-radius: 12px;
}
.upload-form .form-row {
  display: flex;
  gap: 0 20px;
}
.upload-form .form-row .el-form-item {
  flex: 1;
  min-width: 0;
  margin-bottom: 14px;
}
.upload-form .row-2 {
  flex: 2 !important;
}
.upload-form .row-full {
  margin-bottom: 14px;
}
.upload-form :deep(.el-form-item__content) {
  min-width: 0;
}
.upload-form :deep(.el-upload-dragger) {
  padding: 16px 10px;
}
.upload-row {
  align-items: flex-start;
}
.file-item {
  max-width: 620px;
}
.cover-item {
  max-width: 340px;
}
.cover-box {
  display: flex;
  align-items: center;
  gap: 14px;
}
.cover-preview {
  width: 180px;
  aspect-ratio: 16 / 10;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background: #fafafa;
}
.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-icon {
  font-size: 36px;
  color: #c0c4cc;
}
.cover-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.cover-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.action-row {
  margin-top: 4px;
}
</style>
