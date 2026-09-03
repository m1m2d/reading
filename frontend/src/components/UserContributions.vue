<template>
  <div class="contributions" v-loading="loading">
    <template v-if="!loading">
      <div class="section" v-if="data.books && data.books.length">
        <h3>图书投稿（{{ data.books.length }}）</h3>
        <div class="grid">
          <BookCard v-for="book in data.books" :key="book.id" :book="book" />
        </div>
      </div>
      <div class="section" v-if="data.posts && data.posts.length">
        <h3>帖子投稿（{{ data.posts.length }}）</h3>
        <div class="post-list">
          <div v-for="post in data.posts" :key="post.id" class="post-item" @click="$router.push(`/post/${post.id}`)">
            <div class="post-title">{{ post.title }}</div>
            <div class="post-content">{{ post.content }}</div>
            <div class="post-meta">
              <span>{{ post.createdAt }}</span>
              <span><el-icon><Star /></el-icon> {{ post.likeCount }}</span>
              <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!data.books?.length && !data.posts?.length" description="该用户还没有投稿" />
    </template>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import BookCard from './BookCard.vue'
import { getUserContributions } from '../api/users'

const props = defineProps({
  userId: { type: [Number, String], required: true }
})

const loading = ref(false)
const data = ref({ books: [], posts: [] })

async function load() {
  loading.value = true
  try {
    data.value = await getUserContributions(props.userId)
  } finally {
    loading.value = false
  }
}

watch(() => props.userId, load)
onMounted(load)
</script>

<style scoped>
.contributions {
  min-height: 100px;
}
.section {
  margin-bottom: 24px;
}
.section h3 {
  color: #303133;
  margin-bottom: 14px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 200px);
  gap: 20px;
}
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.post-item {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 18px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.post-item:hover {
  border-color: #409eff;
}
.post-title {
  font-weight: 600;
  color: #303133;
}
.post-content {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.post-meta {
  display: flex;
  gap: 16px;
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 8px;
  align-items: center;
}
.post-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
