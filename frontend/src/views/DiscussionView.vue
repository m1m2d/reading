<template>
  <div class="discussion">
    <div class="head">
      <h2>讨论区</h2>
      <div class="head-right">
        <el-input v-model="keyword" placeholder="搜索帖子" clearable style="width: 220px" @keyup.enter="search" />
        <el-button type="primary" @click="openCreate">
          <el-icon><EditPen /></el-icon> 发布帖子
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="posts">
      <div v-for="post in posts" :key="post.id" class="post-card" @click="$router.push(`/post/${post.id}`)">
        <div class="post-head">
          <UserAvatar :user="post" :size="34" clickable @click="$router.push(`/user/${post.userId}`)" />
          <div class="author">
            <span class="nick clickable" @click.stop="$router.push(`/user/${post.userId}`)">{{ post.nickname }}</span>
            <span class="time">{{ post.createdAt }}</span>
          </div>
          <el-button v-if="auth.user?.id === post.userId || auth.isAdmin" size="small" text type="danger"
                     class="delete-btn" @click.stop="remove(post)">删除</el-button>
        </div>
        <div class="post-title">{{ post.title }}</div>
        <div class="post-content">{{ post.content }}</div>
        <div v-if="post.images.length" class="post-images">
          <el-image v-for="img in post.images.slice(0, 3)" :key="img" :src="img" fit="cover"
                    class="thumb" :preview-src-list="post.images" preview-teleported />
          <span v-if="post.images.length > 3" class="more-images">+{{ post.images.length - 3 }}</span>
        </div>
        <div class="post-foot">
          <el-button size="small" text :type="post.liked ? 'warning' : 'default'" @click.stop="like(post)">
            <el-icon><StarFilled v-if="post.liked" /><Star v-else /></el-icon> {{ post.likeCount }}
          </el-button>
          <el-button size="small" text @click.stop="$router.push(`/post/${post.id}`)">
            <el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}
          </el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !posts.length" description="还没有帖子，来发第一帖吧" />
    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :page-size="size"
                     :current-page="page" @current-change="load" />
    </div>

    <el-dialog v-model="createVisible" title="发布帖子" width="600px" :close-on-click-modal="false">
      <el-form :model="form" label-width="60px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入帖子标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="5000" show-word-limit
                    placeholder="分享你的想法..." />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload list-type="picture-card" :auto-upload="false" :limit="9" accept=".jpg,.jpeg,.png,.gif,.webp"
                     :on-change="onImagesChange" :on-remove="onImagesChange" :file-list="fileList">
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">最多 9 张，单张不超过 10MB，支持 JPG/PNG/GIF/WEBP</div>
        </el-form-item>
      </el-form>
      <div class="rate-tip">为防刷屏，同一用户 5 秒内只能发布一个帖子</div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submit">
          {{ creating ? `发布中 ${progress}%` : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPosts, createPost, deletePost, togglePostLike } from '../api/posts'
import { useAuthStore } from '../store/auth'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()
const auth = useAuthStore()
const posts = ref([])
const total = ref(0)
const page = ref(1)
const size = 8
const keyword = ref('')
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const progress = ref(0)
const fileList = ref([])
const form = reactive({ title: '', content: '' })

async function load(p) {
  page.value = p || page.value
  loading.value = true
  try {
    const data = await listPosts({ page: page.value, size, keyword: keyword.value || undefined })
    posts.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  load(1)
}

function openCreate() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  form.title = ''
  form.content = ''
  fileList.value = []
  createVisible.value = true
}

function onImagesChange(uploadFile, uploadFiles) {
  fileList.value = uploadFiles.filter((f) => f.status !== 'removed').map((f) => f.raw || f)
}

async function submit() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入帖子标题')
    return
  }
  creating.value = true
  progress.value = 0
  try {
    const images = fileList.value.map((f) => f.raw || f)
    const post = await createPost(form.title.trim(), form.content.trim(), images, (p) => (progress.value = p))
    ElMessage.success('发布成功')
    createVisible.value = false
    router.push(`/post/${post.id}`)
  } finally {
    creating.value = false
  }
}

async function like(post) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const data = await togglePostLike(post.id)
  post.liked = data.liked
  post.likeCount = data.likeCount
}

async function remove(post) {
  await ElMessageBox.confirm('确定删除该帖子吗？', '提示', { type: 'warning' })
  await deletePost(post.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => load(1))
</script>

<style scoped>
.discussion {
  max-width: 960px;
  margin: 0 auto;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.head h2 {
  font-size: 20px;
  color: #303133;
}
.head-right {
  display: flex;
  gap: 10px;
}
.post-card {
  background: #fff;
  padding: 18px 22px;
  margin-bottom: 14px;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  transition: border-color 0.2s;
}
.post-card:hover {
  border-color: #409eff;
}
.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.author {
  display: flex;
  flex-direction: column;
}
.nick {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
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
.delete-btn {
  margin-left: auto;
}
.post-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin: 12px 0 6px;
}
.post-content {
  color: #606266;
  line-height: 1.7;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
}
.post-images {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  position: relative;
}
.thumb {
  width: 130px;
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  cursor: pointer;
}
.more-images {
  position: absolute;
  right: 6px;
  bottom: 6px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 13px;
}
.post-foot {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}
.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
.rate-tip {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
