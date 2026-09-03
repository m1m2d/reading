<template>
  <div class="following-page">
    <div class="head">
      <h2>我的关注</h2>
      <div class="select-wrap" v-if="followings.length">
        <el-select v-model="selectedId" placeholder="选择关注者查看其投稿" filterable style="width: 320px">
          <el-option v-for="u in followings" :key="u.id" :value="u.id" :label="u.nickname || u.username">
            <div class="option-item">
              <el-avatar :size="22" :src="u.avatarUrl || undefined">{{ (u.nickname || u.username || 'U')[0] }}</el-avatar>
              <span>{{ u.nickname || u.username }}</span>
              <span class="opt-username">@{{ u.username }}</span>
            </div>
          </el-option>
        </el-select>
        <el-button v-if="selectedUser" text type="primary" @click="$router.push(`/user/${selectedUser.id}`)">
          进入 {{ selectedUser.nickname || selectedUser.username }} 的主页
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !followings.length" description="还没有关注任何人，去图书卡片或用户主页点“关注”吧" />

    <div v-else-if="selectedUser" class="card">
      <div class="selected-head">
        <el-avatar :size="40" :src="selectedUser.avatarUrl || undefined">
          {{ (selectedUser.nickname || selectedUser.username || 'U')[0] }}
        </el-avatar>
        <div>
          <div class="selected-name">{{ selectedUser.nickname || selectedUser.username }}</div>
          <div class="selected-meta">@{{ selectedUser.username }} 的所有投稿</div>
        </div>
      </div>
      <UserContributions :user-id="selectedUser.id" />
    </div>
    <el-empty v-else-if="followings.length" description="请选择一位关注者查看投稿" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import UserContributions from '../components/UserContributions.vue'
import { getFollowing } from '../api/users'

const followings = ref([])
const selectedId = ref(null)
const loading = ref(false)

const selectedUser = computed(() => followings.value.find((u) => u.id === selectedId.value))

onMounted(async () => {
  loading.value = true
  try {
    const data = await getFollowing({ page: 1, size: 200 })
    followings.value = data.records
    if (followings.value.length) {
      selectedId.value = followings.value[0].id
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.following-page {
  max-width: 1080px;
  margin: 0 auto;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}
.head h2 {
  font-size: 20px;
  color: #303133;
}
.select-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}
.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.opt-username {
  color: #c0c4cc;
  font-size: 12px;
}
.card {
  background: #fff;
  padding: 22px 26px;
  border: 1px solid #e4e7ed;
}
.selected-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}
.selected-name {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}
.selected-meta {
  color: #909399;
  font-size: 13px;
  margin-top: 2px;
}
</style>
