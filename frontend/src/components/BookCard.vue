<template>
  <div class="book-card" @click="$router.push(`/book/${book.id}`)">
    <div class="cover-wrap">
      <img v-if="book.coverUrl" :src="book.coverUrl" class="cover" alt="封面" />
      <div v-else class="cover cover-placeholder">云阅</div>
      <span v-if="book.status === 0" class="status-tag">待审核</span>
      <span v-else-if="book.status === 2" class="status-tag rejected">已驳回</span>
    </div>
    <div class="info">
      <div class="title" :title="book.title">{{ book.title }}</div>
      <div class="meta">
        <span>{{ book.author || '佚名' }}</span>
        <span v-if="book.categoryName">{{ book.categoryName }}</span>
      </div>
      <div class="publisher" v-if="book.uploaderId" @click.stop="goUser">
        <span class="pub-label">发布者：</span>
        <el-avatar :size="20" :src="book.uploaderAvatar || undefined" class="pub-avatar">
          {{ (book.uploaderName || 'U')[0] }}
        </el-avatar>
        <span class="pub-name">{{ book.uploaderName }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  book: { type: Object, required: true }
})
const router = useRouter()

function goUser() {
  router.push(`/user/${props.book.uploaderId}`)
}
</script>

<style scoped>
.book-card {
  width: 200px;
  cursor: pointer;
  background: #fff;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}
.cover-wrap {
  position: relative;
  width: 200px;
  aspect-ratio: 16 / 10;
  background: #eef1f5;
}
.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4b6cb7, #182848);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
}
.status-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #e6a23c;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}
.status-tag.rejected {
  background: #f56c6c;
}
.info {
  padding: 10px 12px;
}
.title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.publisher {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
  cursor: pointer;
  min-width: 0;
}
.pub-label {
  white-space: nowrap;
}
.pub-avatar {
  background: #c0c4cc;
  color: #fff;
  flex-shrink: 0;
}
.pub-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #606266;
}
.publisher:hover .pub-name {
  color: #409eff;
}
</style>
