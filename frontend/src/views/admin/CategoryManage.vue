<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog(null)">新增根分类</el-button>
      <span class="hint">最多支持两级分类</span>
    </div>
    <el-tree :data="tree" :props="{ label: 'name', children: 'children' }" node-key="id" default-expand-all>
      <template #default="{ data }">
        <div class="node">
          <span>{{ data.name }}</span>
          <span class="node-actions">
            <el-button size="small" text type="primary" @click="openDialog(data, true)">新增子分类</el-button>
            <el-button size="small" text type="warning" @click="openDialog(data)">编辑</el-button>
            <el-button size="small" text type="danger" @click="remove(data)">删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <el-dialog v-model="dialogVisible" :title="dialogForm.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="dialogForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="dialogForm.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="dialogForm.parentId" clearable placeholder="无（根分类）" style="width: 100%">
            <el-option v-for="n in tree" :key="n.id" :label="n.name" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialogForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../../api/admin'

const tree = ref([])
const dialogVisible = ref(false)
const dialogForm = reactive({ id: null, name: '', parentId: null, sort: 0 })

async function load() {
  tree.value = await getCategories()
}

function openDialog(data, isChild) {
  if (data) {
    dialogForm.id = data.id
    dialogForm.name = data.name
    dialogForm.parentId = isChild ? data.id : data.parentId
    dialogForm.sort = data.sort || 0
  } else {
    Object.assign(dialogForm, { id: null, name: '', parentId: null, sort: 0 })
  }
  dialogVisible.value = true
}

async function save() {
  if (!dialogForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (dialogForm.id) {
    await updateCategory(dialogForm.id, {
      name: dialogForm.name.trim(),
      parentId: dialogForm.parentId,
      sort: dialogForm.sort
    })
  } else {
    await createCategory({
      name: dialogForm.name.trim(),
      parentId: dialogForm.parentId,
      sort: dialogForm.sort
    })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(data) {
  await ElMessageBox.confirm(`确定删除分类「${data.name}」吗？`, '提示', { type: 'warning' })
  await deleteCategory(data.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 8px;
}
.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
}
.node:hover .node-actions {
  opacity: 1;
}
</style>
