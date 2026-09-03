<template>
  <el-card>
    <template #header>
      <div class="card-head">
        <span>系统配置</span>
        <span class="hint">保存后立即生效（配置已缓存，最迟 60 秒内全局生效）</span>
      </div>
    </template>
    <el-form label-width="220px" style="max-width: 720px">
      <el-form-item v-for="item in configs" :key="item.configKey" :label="labelOf(item.configKey)">
        <template v-if="isBool(item.configKey)">
          <el-switch v-model="item.configValue" active-value="true" inactive-value="false" />
        </template>
        <template v-else-if="isNumber(item.configKey)">
          <el-input-number v-model="item.configValue" :min="0" />
        </template>
        <template v-else>
          <el-input v-model="item.configValue" />
        </template>
        <div class="desc">{{ descOf(item.configKey) }}</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfig, updateConfig } from '../../api/admin'

const configs = ref([])
const saving = ref(false)

const labels = {
  'upload.maxSizeMb': '电子书大小上限（MB）',
  allowedFormats: '允许的格式白名单',
  reviewEnabled: '新书上传需审核',
  registerEnabled: '开放自动注册',
  'cover.maxSizeMb': '封面大小上限（MB）',
  allowedCoverFormats: '允许的封面格式',
  chunkThresholdMb: '分片上传阈值（MB）'
}
const descs = {
  allowedFormats: '逗号分隔：pdf,epub,txt,mobi',
  allowedCoverFormats: '逗号分隔：jpg,png',
  reviewEnabled: '关闭后新上传书籍自动上架',
  chunkThresholdMb: '超过该大小前端自动分片上传'
}
const bools = ['reviewEnabled', 'registerEnabled']
const numbers = ['upload.maxSizeMb', 'cover.maxSizeMb', 'chunkThresholdMb']

function labelOf(key) {
  return labels[key] || key
}
function descOf(key) {
  return descs[key] || ''
}
function isBool(key) {
  return bools.includes(key)
}
function isNumber(key) {
  return numbers.includes(key)
}

async function load() {
  configs.value = await getConfig()
  configs.value.forEach((c) => {
    if (isNumber(c.configKey)) c.configValue = Number(c.configValue)
  })
}

async function save() {
  saving.value = true
  try {
    const payload = configs.value.map((c) => ({
      configKey: c.configKey,
      configValue: String(c.configValue)
    }))
    await updateConfig(payload)
    ElMessage.success('配置已保存')
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  gap: 16px;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.desc {
  color: #c0c4cc;
  font-size: 12px;
  width: 100%;
}
</style>
