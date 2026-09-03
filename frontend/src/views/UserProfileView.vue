<template>
  <div class="user-profile" v-if="profile">
    <div class="user-card">
      <UserAvatar :user="profile" :size="72" />
      <div class="user-info">
        <div class="name">
          {{ profile.nickname || profile.username }}
          <el-tag v-if="profile.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
        </div>
        <div class="meta">用户名：{{ profile.username }} · 注册于 {{ profile.createdAt }}</div>
        <div class="stats">
          <span>关注 {{ profile.followCount }}</span>
          <span>粉丝 {{ profile.followerCount }}</span>
          <span>图书 {{ profile.bookCount }}</span>
          <span>帖子 {{ profile.postCount }}</span>
        </div>
      </div>
      <div class="actions">
        <el-button v-if="isSelf" type="info" disabled>这是我</el-button>
        <template v-else>
          <el-button v-if="auth.isLoggedIn" :type="profile.followedByMe ? 'default' : 'primary'"
                     :loading="following" @click="follow">
            {{ profile.followedByMe ? '取消关注' : '+ 关注' }}
          </el-button>
          <el-button v-else type="primary" @click="$router.push('/login')">登录后关注</el-button>
        </template>
      </div>
    </div>

    <div class="card contributions-card">
      <h2>TA 的所有投稿</h2>
      <UserContributions :user-id="route.params.id" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserAvatar from '../components/UserAvatar.vue'
import UserContributions from '../components/UserContributions.vue'
import { getUser, toggleFollow } from '../api/users'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()
const profile = ref(null)
const following = ref(false)

const isSelf = computed(() => profile.value && auth.user?.id === profile.value.id)

async function load() {
  profile.value = await getUser(route.params.id)
}

async function follow() {
  following.value = true
  try {
    const data = await toggleFollow(profile.value.id)
    profile.value.followedByMe = data.followed
    profile.value.followerCount = data.followerCount
    ElMessage.success(data.followed ? '关注成功' : '已取消关注')
  } finally {
    following.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.user-profile {
  max-width: 1080px;
  margin: 0 auto;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 22px;
  background: #fff;
  padding: 26px 30px;
  border: 1px solid #e4e7ed;
  margin-bottom: 18px;
}
.user-info {
  flex: 1;
  min-width: 0;
}
.name {
  font-size: 22px;
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
.stats {
  display: flex;
  gap: 20px;
  margin-top: 12px;
  color: #606266;
  font-size: 13px;
}
.contributions-card {
  background: #fff;
  padding: 22px 26px;
  border: 1px solid #e4e7ed;
}
.contributions-card h2 {
  font-size: 17px;
  color: #303133;
  margin-bottom: 18px;
}
</style>
