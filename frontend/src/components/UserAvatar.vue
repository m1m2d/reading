<template>
  <el-avatar
    :size="size"
    :src="user?.avatarUrl || undefined"
    :class="{ clickable: clickable }"
    class="user-avatar"
    @click="onClick"
  >
    {{ fallback }}
  </el-avatar>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  user: { type: Object, default: null },
  size: { type: Number, default: 32 },
  clickable: { type: Boolean, default: false }
})

const emit = defineEmits(['click'])

const fallback = computed(() => {
  const name = props.user?.nickname || props.user?.username || 'U'
  return name[0]
})

function onClick() {
  if (props.clickable) {
    emit('click')
  }
}
</script>

<style scoped>
.user-avatar {
  background: #c0c4cc;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}
.user-avatar.clickable {
  cursor: pointer;
}
</style>
