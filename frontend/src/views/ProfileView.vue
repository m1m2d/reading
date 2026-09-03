<template>
  <div class="profile">
    <div class="user-card">
      <div class="avatar-wrap" @click="openAvatar">
        <UserAvatar :user="auth.user" :size="64" />
        <div class="avatar-mask">
          <el-icon><Camera /></el-icon>
          更换头像
        </div>
      </div>
      <div class="user-info">
        <div class="name">
          {{ auth.user?.nickname || auth.user?.username }}
          <el-tag v-if="auth.isAdmin" type="danger" size="small">管理员</el-tag>
        </div>
        <div class="meta">用户名：{{ auth.user?.username }} · 注册于 {{ auth.user?.createdAt }}</div>
      </div>
    </div>

    <el-dialog v-model="avatarVisible" title="设置头像" width="420px">
      <div class="avatar-dialog">
        <el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp"
                   :on-change="onAvatarChange">
          <div class="avatar-upload-box">
            <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview" alt="头像预览" />
            <div v-else class="avatar-upload-tip">
              <el-icon><Plus /></el-icon>
              <span>选择图片</span>
            </div>
          </div>
        </el-upload>
        <div class="avatar-hint">支持 JPG/PNG/WEBP，不超过 5MB，保存后全局生效</div>
      </div>
      <template #footer>
        <el-button @click="avatarVisible = false">取消</el-button>
        <el-button type="primary" :loading="avatarSaving" @click="saveAvatar">保存头像</el-button>
      </template>
    </el-dialog>

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane label="我的收藏" name="favorites">
        <div v-loading="favLoading" class="grid" v-if="favorites.length">
          <div v-for="book in favorites" :key="book.id" class="fav-item">
            <BookCard :book="book" />
            <el-button size="small" type="danger" text class="remove" @click="removeFavorite(book)">取消收藏</el-button>
          </div>
        </div>
        <el-empty v-else-if="!favLoading" description="还没有收藏任何书籍" />
        <div class="pager">
          <el-pagination layout="prev, pager, next, total" :total="favTotal" :page-size="favSize"
                         :current-page="favPage" @current-change="loadFavorites" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的上传" name="uploads">
        <el-table :data="uploads" v-loading="uploadLoading" border stripe>
          <el-table-column prop="title" label="书名" min-width="180" />
          <el-table-column prop="author" label="作者" width="110" />
          <el-table-column label="分类" width="110">
            <template #default="{ row }">{{ row.categoryName || '未分类' }}</template>
          </el-table-column>
          <el-table-column label="格式" width="70">
            <template #default="{ row }">{{ (row.fileFormat || '').toUpperCase() }}</template>
          </el-table-column>
          <el-table-column label="版本" width="70">
            <template #default="{ row }">v{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
                {{ row.status === 1 ? '已上架' : row.status === 2 ? '已驳回' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="160" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="$router.push(`/book/${row.id}`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="别人的评论" name="received">
        <div v-loading="receivedLoading">
          <div v-for="item in received" :key="`${item.source}-${item.id}`" class="received-item">
            <el-tag :type="item.source === 'post' ? 'primary' : 'success'" size="small">
              {{ item.source === 'post' ? '帖子' : '书籍' }}
            </el-tag>
            <div class="received-main">
              <div class="received-title">
                我的{{ item.source === 'post' ? '帖子' : '书籍' }}《{{ item.sourceTitle }}》
                <span class="commenter">{{ item.commenter }}</span>
              </div>
              <div class="received-content">{{ item.content }}</div>
              <div class="received-time">{{ item.createdAt }}</div>
            </div>
            <el-button size="small" text type="primary" class="goto"
                       @click="$router.push(item.source === 'post' ? `/post/${item.sourceId}` : `/book/${item.sourceId}`)">
              查看
            </el-button>
          </div>
          <el-empty v-if="!receivedLoading && !received.length" description="还没有收到评论" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import BookCard from '../components/BookCard.vue'
import UserAvatar from '../components/UserAvatar.vue'
import { listFavorites, toggleFavorite } from '../api/favorites'
import { myUploads } from '../api/books'
import { receivedComments } from '../api/posts'
import { uploadAvatar } from '../api/users'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()
const tab = ref(route.query.tab || 'favorites')

const favorites = ref([])
const favTotal = ref(0)
const favPage = ref(1)
const favSize = 12
const favLoading = ref(false)

const uploads = ref([])
const uploadLoading = ref(false)

const received = ref([])
const receivedLoading = ref(false)
const avatarVisible = ref(false)
const avatarPreview = ref('')
const avatarFile = ref(null)
const avatarSaving = ref(false)

async function loadFavorites(p) {
  favPage.value = p || favPage.value
  favLoading.value = true
  try {
    const data = await listFavorites(favPage.value, favSize)
    favorites.value = data.records
    favTotal.value = data.total
  } finally {
    favLoading.value = false
  }
}

async function removeFavorite(book) {
  await toggleFavorite(book.id)
  ElMessage.success('已取消收藏')
  loadFavorites()
}

async function loadUploads() {
  uploadLoading.value = true
  try {
    uploads.value = await myUploads()
  } finally {
    uploadLoading.value = false
  }
}

async function loadReceived() {
  receivedLoading.value = true
  try {
    received.value = await receivedComments()
  } finally {
    receivedLoading.value = false
  }
}

watch(tab, (t) => {
  if (t === 'favorites') loadFavorites(1)
  if (t === 'uploads') loadUploads()
  if (t === 'received') loadReceived()
})

onMounted(() => {
  auth.fetchMe()
  loadFavorites(1)
})

function openAvatar() {
  avatarFile.value = null
  avatarPreview.value = ''
  avatarVisible.value = true
}

function onAvatarChange(file) {
  const raw = file.raw
  if (!/\.(jpg|jpeg|png|webp)$/i.test(raw.name)) {
    ElMessage.warning('头像仅支持 JPG/PNG/WEBP 格式')
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return
  }
  avatarFile.value = raw
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
  avatarPreview.value = URL.createObjectURL(raw)
}

async function saveAvatar() {
  if (!avatarFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  avatarSaving.value = true
  try {
    const user = await uploadAvatar(avatarFile.value)
    auth.setAvatar(user.avatarUrl)
    ElMessage.success('头像已更新')
    avatarVisible.value = false
  } finally {
    avatarSaving.value = false
  }
}
</script>

<style scoped>
.profile {
  max-width: 1080px;
  margin: 0 auto;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #fff;
  padding: 24px 28px;
  border: 1px solid #e4e7ed;
  margin-bottom: 18px;
}
.avatar-wrap {
  position: relative;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}
.avatar-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.avatar-upload-box {
  width: 180px;
  aspect-ratio: 1;
  border: 1px dashed #dcdfe6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;
}
.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-upload-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}
.avatar-hint {
  color: #909399;
  font-size: 12px;
}
.name {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}
.meta {
  color: #909399;
  font-size: 13px;
  margin-top: 6px;
}
.tabs {
  background: #fff;
  padding: 8px 24px 20px;
  border: 1px solid #e4e7ed;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 200px);
  gap: 22px;
  min-height: 120px;
}
.fav-item {
  position: relative;
}
.remove {
  margin-top: 6px;
}
.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
.received-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f0f2f5;
}
.received-main {
  flex: 1;
  min-width: 0;
}
.received-title {
  color: #606266;
  font-size: 14px;
}
.received-title .commenter {
  color: #409eff;
  margin-left: 8px;
  font-weight: 600;
}
.received-content {
  color: #303133;
  margin-top: 6px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.received-time {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 6px;
}
.goto {
  flex-shrink: 0;
}
</style>
