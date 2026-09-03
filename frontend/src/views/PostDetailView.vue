<template>
  <div class="post-detail" v-if="post">
    <div class="card">
      <div class="back-row">
        <el-button text @click="$router.push('/discussion')">
          <el-icon><Back /></el-icon> 返回讨论区
        </el-button>
        <el-button v-if="auth.user?.id === post.userId || auth.isAdmin" text type="danger" @click="removePost">
          删除帖子
        </el-button>
      </div>
      <div class="post-head">
        <UserAvatar :user="post" :size="42" clickable @click="$router.push(`/user/${post.userId}`)" />
        <div>
          <div class="nick clickable" @click="$router.push(`/user/${post.userId}`)">
            {{ post.nickname }} <span class="username">@{{ post.username }}</span>
          </div>
          <div class="time">{{ post.createdAt }}</div>
        </div>
      </div>
      <h1 class="title">{{ post.title }}</h1>
      <div class="content">{{ post.content }}</div>
      <div v-if="post.images.length" class="images">
        <el-image v-for="img in post.images" :key="img" :src="img" fit="contain"
                  class="big-image" :preview-src-list="post.images" preview-teleported />
      </div>
      <div class="like-row">
        <el-button :type="post.liked ? 'warning' : 'default'" @click="like">
          <el-icon><StarFilled v-if="post.liked" /><Star v-else /></el-icon>
          {{ post.liked ? '已点赞' : '点赞' }}（{{ post.likeCount }}）
        </el-button>
      </div>
    </div>

    <div class="card comments">
      <h3>评论（{{ totalComments }}）</h3>
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
              <el-button size="small" text :type="c.liked ? 'warning' : 'default'" @click="likeComment(c)">
                <el-icon><StarFilled v-if="c.liked" /><Star v-else /></el-icon> {{ c.likeCount }}
              </el-button>
              <el-button size="small" text @click="startReply(c)">回复</el-button>
              <el-button v-if="auth.user?.id === c.userId || auth.isAdmin" size="small" text type="danger"
                         @click="removeComment(c)">删除</el-button>
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
                  <el-button size="small" text :type="r.liked ? 'warning' : 'default'" @click="likeComment(r)">
                    <el-icon><StarFilled v-if="r.liked" /><Star v-else /></el-icon> {{ r.likeCount }}
                  </el-button>
                  <el-button v-if="auth.user?.id === r.userId || auth.isAdmin" size="small" text type="danger"
                             @click="removeComment(r)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="!commentsLoading && !comments.length" description="暂无评论，来说两句" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPost, deletePost, togglePostLike, listPostComments, addPostComment,
  togglePostCommentLike, deletePostComment
} from '../api/posts'
import { useAuthStore } from '../store/auth'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const post = ref(null)
const comments = ref([])
const totalComments = ref(0)
const newComment = ref('')
const commentLoading = ref(false)
const commentsLoading = ref(false)

async function load() {
  post.value = await getPost(route.params.id)
  loadComments()
}

async function loadComments() {
  commentsLoading.value = true
  try {
    comments.value = await listPostComments(route.params.id)
    totalComments.value = comments.value.reduce((sum, c) => sum + 1 + c.children.length, 0)
  } finally {
    commentsLoading.value = false
  }
}

async function like() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const data = await togglePostLike(post.value.id)
  post.value.liked = data.liked
  post.value.likeCount = data.likeCount
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
    await addPostComment(post.value.id, { content: newComment.value.trim() })
    newComment.value = ''
    ElMessage.success('评论成功')
    loadComments()
    post.value.commentCount += 1
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
    await addPostComment(post.value.id, { content: value, parentId: c.id })
    ElMessage.success('回复成功')
    loadComments()
    post.value.commentCount += 1
  }).catch(() => {})
}

async function likeComment(c) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const data = await togglePostCommentLike(c.id)
  c.liked = data.liked
  c.likeCount = data.likeCount
}

async function removeComment(c) {
  await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
  await deletePostComment(c.id)
  ElMessage.success('已删除')
  loadComments()
}

async function removePost() {
  await ElMessageBox.confirm('确定删除该帖子吗？', '提示', { type: 'warning' })
  await deletePost(post.value.id)
  ElMessage.success('已删除')
  router.push('/discussion')
}

onMounted(load)
</script>

<style scoped>
.post-detail {
  max-width: 900px;
  margin: 0 auto;
}
.card {
  background: #fff;
  padding: 24px 28px;
  border: 1px solid #e4e7ed;
  margin-bottom: 16px;
}
.back-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}
.post-head {
  display: flex;
  align-items: center;
  gap: 12px;
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
.username {
  color: #c0c4cc;
  font-size: 12px;
  font-weight: 400;
}
.time {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 2px;
}
.title {
  font-size: 24px;
  color: #303133;
  margin: 18px 0 10px;
}
.content {
  color: #4a4a4a;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 16px;
}
.images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.big-image {
  width: 260px;
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  cursor: zoom-in;
}
.like-row {
  border-top: 1px solid #f0f2f5;
  padding-top: 14px;
}
.comments h3 {
  color: #303133;
  margin-bottom: 14px;
}
.comment-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 18px;
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
</style>
