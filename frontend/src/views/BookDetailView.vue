<template>
  <div class="detail-page" v-if="book">
    <div class="body">
      <div class="book-panel">
        <div class="cover-wrap">
          <img :src="book.coverUrl" class="cover" alt="封面" />
        </div>
        <div class="meta">
          <h1>{{ book.title }}</h1>
          <div class="sub">
            <span>作者：{{ book.author || '佚名' }}</span>
            <span v-if="book.isbn">ISBN：{{ book.isbn }}</span>
            <span>分类：{{ book.categoryName || '未分类' }}</span>
          </div>
          <div class="sub">
            <span>格式：{{ (book.fileFormat || '').toUpperCase() }}</span>
            <span>大小：{{ formatSize(book.fileSize) }}</span>
            <span>版本：v{{ book.versionNo }}</span>
            <span>下载：{{ book.downloadCount }}</span>
          </div>
          <div class="sub" v-if="book.uploaderName">
            <span>上传者：{{ book.uploaderName }}（{{ book.uploadIp }}）</span>
            <span>上传时间：{{ book.createdAt }}</span>
          </div>
          <el-tag v-if="book.status === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="book.status === 2" type="danger">已驳回</el-tag>
          <el-tag v-else type="success">已上架</el-tag>

          <div class="actions">
            <el-button type="primary" size="large" :disabled="book.status !== 1" @click="read">
              <el-icon><Reading /></el-icon> 在线阅读
            </el-button>
            <el-button size="large" :disabled="book.status !== 1" @click="download">
              <el-icon><Download /></el-icon> 下载原文件
            </el-button>
            <el-button size="large" :type="book.favorite ? 'warning' : 'default'" @click="toggleFav">
              <el-icon><StarFilled v-if="book.favorite" /><Star v-else /></el-icon>
              {{ book.favorite ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="large" @click="traceVisible = true">
              <el-icon><Connection /></el-icon> 书籍溯源
            </el-button>
          </div>
        </div>
      </div>

      <div class="desc-panel">
        <h3>内容简介</h3>
        <p>{{ book.description || '暂无简介' }}</p>
      </div>

      <div class="comments-panel">
        <h3>评论区（{{ totalComments }}）</h3>
        <div class="comment-input">
          <el-input v-model="newComment" type="textarea" :rows="3" placeholder="发表你的看法..."
                    maxlength="1000" show-word-limit />
          <el-button type="primary" :loading="commentLoading" @click="submitComment">发表评论</el-button>
        </div>
        <div v-loading="commentsLoading">
          <div v-for="c in comments" :key="c.id" class="comment">
            <div class="comment-main">
              <div class="comment-head">
                <UserAvatar :user="c" :size="28" clickable @click="$router.push(`/user/${c.userId}`)" />
                <span class="nick clickable" @click="$router.push(`/user/${c.userId}`)">{{ c.nickname }}</span>
                <span class="time">{{ c.createdAt }}</span>
              </div>
              <div class="comment-content">{{ c.content }}</div>
              <div class="comment-actions">
                <el-button size="small" text :type="c.liked ? 'warning' : 'default'" @click="like(c)">
                  <el-icon><StarFilled v-if="c.liked" /><Star v-else /></el-icon> {{ c.likeCount }}
                </el-button>
                <el-button size="small" text @click="startReply(c)">回复</el-button>
                <el-button v-if="auth.user?.id === c.userId || auth.isAdmin" size="small" text type="danger"
                           @click="remove(c)">删除</el-button>
              </div>
            </div>
            <div v-if="c.children.length" class="replies">
              <div v-for="r in c.children" :key="r.id" class="comment reply">
                <div class="comment-main">
                  <div class="comment-head">
                    <UserAvatar :user="r" :size="24" clickable @click="$router.push(`/user/${r.userId}`)" />
                    <span class="nick clickable" @click="$router.push(`/user/${r.userId}`)">{{ r.nickname }}</span>
                    <span class="time">{{ r.createdAt }}</span>
                  </div>
                  <div class="comment-content">{{ r.content }}</div>
                  <div class="comment-actions">
                    <el-button size="small" text :type="r.liked ? 'warning' : 'default'" @click="like(r)">
                      <el-icon><StarFilled v-if="r.liked" /><Star v-else /></el-icon> {{ r.likeCount }}
                    </el-button>
                    <el-button v-if="auth.user?.id === r.userId || auth.isAdmin" size="small" text type="danger"
                               @click="remove(r)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="!commentsLoading && !comments.length" description="暂无评论，快来抢沙发" />
        </div>
      </div>
    </div>

    <el-dialog v-model="traceVisible" title="书籍溯源与防篡改校验" width="720px">
      <div class="trace-panel" v-if="trace">
        <div class="trace-summary">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="书名">{{ trace.title }}</el-descriptions-item>
            <el-descriptions-item label="当前版本">v{{ trace.versionNo }}</el-descriptions-item>
            <el-descriptions-item label="文件哈希" :span="2">
              <code class="hash">{{ trace.fileHash }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="上传者">{{ trace.uploaderName }}（{{ trace.uploadIp }}）</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ trace.createdAt }}</el-descriptions-item>
          </el-descriptions>
          <el-button type="warning" :loading="verifying" class="verify-btn" @click="verify">
            <el-icon><CircleCheck /></el-icon> 校验文件完整性
          </el-button>
          <el-alert v-if="verifyResult" :type="verifyResult.consistent ? 'success' : 'error'"
                    :title="verifyResult.message" :closable="false" class="verify-alert">
            <template #default>
              记录哈希：{{ verifyResult.recordedHash }}<br />
              实时哈希：{{ verifyResult.currentHash }}
            </template>
          </el-alert>
        </div>
        <el-timeline class="timeline">
          <el-timeline-item v-for="log in trace.traceLogs" :key="log.id" :timestamp="log.createdAt"
                            :type="log.action === 'VERIFY' ? 'success' : 'primary'">
            <b>{{ actionText(log.action) }}</b>
            <div class="detail" v-if="log.detail">{{ log.detail }}</div>
          </el-timeline-item>
        </el-timeline>
        <h4>版本历史</h4>
        <el-table :data="trace.versions" size="small" border>
          <el-table-column prop="versionNo" label="版本" width="70" />
          <el-table-column prop="fileHash" label="文件哈希" show-overflow-tooltip />
          <el-table-column prop="changeLog" label="变更说明" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="归档时间" width="160" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBook, getTrace, verifyBook } from '../api/books'
import { listComments, addComment, toggleLike, deleteComment } from '../api/comments'
import { toggleFavorite } from '../api/favorites'
import { useAuthStore } from '../store/auth'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const auth = useAuthStore()
const book = ref(null)
const trace = ref(null)
const comments = ref([])
const totalComments = ref(0)
const newComment = ref('')
const commentLoading = ref(false)
const commentsLoading = ref(false)
const traceVisible = ref(false)
const verifying = ref(false)
const verifyResult = ref(null)

async function load() {
  book.value = await getBook(route.params.id)
  loadComments()
}

async function loadComments() {
  commentsLoading.value = true
  try {
    comments.value = await listComments(route.params.id)
    totalComments.value = comments.value.reduce((sum, c) => sum + 1 + c.children.length, 0)
  } finally {
    commentsLoading.value = false
  }
}

async function loadTrace() {
  trace.value = await getTrace(route.params.id)
}

function read() {
  window.open(`/reader/${book.value.id}`, '_blank')
}

function download() {
  window.open(`/api/v1/books/${book.value.id}/download`, '_blank')
}

async function toggleFav() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const data = await toggleFavorite(book.value.id)
  book.value.favorite = data.favorited
  ElMessage.success(data.favorited ? '收藏成功' : '已取消收藏')
}

async function submitComment() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  commentLoading.value = true
  try {
    await addComment({ bookId: Number(route.params.id), content: newComment.value.trim() })
    newComment.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } finally {
    commentLoading.value = false
  }
}

function startReply(c) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  ElMessageBox.prompt(`回复 @${c.nickname}：`, '回复评论', {
    confirmButtonText: '发布',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '内容不能为空'
  }).then(async ({ value }) => {
    await addComment({ bookId: Number(route.params.id), content: value, parentId: c.id })
    ElMessage.success('回复成功')
    loadComments()
  }).catch(() => {})
}

async function like(c) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const data = await toggleLike(c.id)
  c.liked = data.liked
  c.likeCount = data.likeCount
}

async function remove(c) {
  await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
  await deleteComment(c.id)
  ElMessage.success('已删除')
  loadComments()
}

async function verify() {
  verifying.value = true
  try {
    verifyResult.value = await verifyBook(route.params.id)
    if (!verifyResult.value.consistent) {
      ElMessage.error(verifyResult.value.message)
    } else {
      ElMessage.success(verifyResult.value.message)
    }
    loadTrace()
  } finally {
    verifying.value = false
  }
}

function actionText(action) {
  const map = {
    UPLOAD: '上传', UPDATE_VERSION: '版本更新', APPROVE: '审核通过', REJECT: '审核驳回',
    DOWNLOAD: '下载', VERIFY: '完整性校验', DELETE: '删除'
  }
  return map[action] || action
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

onMounted(() => {
  load()
  loadTrace()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
}
.body {
  max-width: 1080px;
  margin: 20px auto;
  padding: 0 8px;
}
.book-panel {
  display: flex;
  gap: 32px;
  background: #fff;
  padding: 28px;
  border: 1px solid #e4e7ed;
}
.cover-wrap {
  width: 360px;
  flex-shrink: 0;
}
.cover {
  width: 360px;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}
.meta {
  flex: 1;
}
.meta h1 {
  font-size: 26px;
  color: #303133;
  margin-bottom: 14px;
}
.sub {
  display: flex;
  gap: 20px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.actions {
  margin-top: 28px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.desc-panel, .comments-panel {
  background: #fff;
  padding: 24px;
  margin-top: 20px;
  border: 1px solid #e4e7ed;
}
.desc-panel h3, .comments-panel h3 {
  color: #303133;
  margin-bottom: 12px;
}
.desc-panel p {
  color: #606266;
  line-height: 1.9;
  white-space: pre-wrap;
}
.comment-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 20px;
}
.comment-input .el-input {
  flex: 1;
}
.comment {
  border-bottom: 1px solid #f0f2f5;
  padding: 14px 0;
}
.comment.reply {
  padding-left: 24px;
  border-bottom: none;
  padding-top: 10px;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nick {
  font-weight: 600;
  color: #303133;
}
.nick.clickable {
  cursor: pointer;
}
.nick.clickable:hover {
  color: #409eff;
}
.time {
  color: #c0c4cc;
  font-size: 12px;
}
.comment-content {
  color: #4a4a4a;
  line-height: 1.7;
  margin: 8px 0 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-actions {
  display: flex;
  gap: 4px;
}
.replies {
  margin-left: 24px;
  background: #fafbfc;
  border-radius: 8px;
  padding: 0 14px;
}
.trace-panel {
  max-height: 70vh;
  overflow-y: auto;
}
.hash {
  font-size: 12px;
  word-break: break-all;
}
.verify-btn {
  margin-top: 14px;
}
.verify-alert {
  margin-top: 12px;
}
.timeline {
  margin: 20px 0;
}
.detail {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  word-break: break-all;
}
.trace-panel h4 {
  margin: 16px 0 8px;
}
</style>
