<template>
  <div class="categories-page">
    <el-tabs v-model="rootTab" class="root-tabs" @tab-change="onRootChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane v-for="root in categoryStore.tree" :key="root.id" :label="root.name"
                   :name="String(root.id)" />
    </el-tabs>
    <el-tabs v-if="currentRoot && currentRoot.children && currentRoot.children.length"
             v-model="childTab" class="child-tabs" @tab-change="load(1)">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane v-for="child in currentRoot.children" :key="child.id" :label="child.name"
                   :name="String(child.id)" />
    </el-tabs>

    <h2 class="page-title">{{ currentTitle }}</h2>
    <div v-loading="loading" class="grid" v-if="books.length">
      <BookCard v-for="book in books" :key="book.id" :book="book" />
    </div>
    <el-empty v-else-if="!loading" description="该分类下暂无书籍" />
    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :page-size="size"
                     :current-page="page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BookCard from '../components/BookCard.vue'
import { listBooks } from '../api/books'
import { useCategoryStore } from '../store/categories'

const categoryStore = useCategoryStore()
const rootTab = ref('all')
const childTab = ref('all')
const books = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)

const currentRoot = computed(() =>
  categoryStore.tree.find((root) => String(root.id) === rootTab.value)
)

const effectiveCategoryId = computed(() => {
  if (rootTab.value === 'all') return null
  if (currentRoot.value?.children?.length && childTab.value !== 'all') {
    return Number(childTab.value)
  }
  return Number(rootTab.value)
})

const currentTitle = computed(() => {
  if (rootTab.value === 'all') return '全部书籍'
  const root = currentRoot.value
  if (root && childTab.value !== 'all') {
    const child = (root.children || []).find((c) => String(c.id) === childTab.value)
    if (child) return child.name
  }
  return root ? root.name : '全部书籍'
})

function onRootChange() {
  childTab.value = 'all'
  load(1)
}

async function load(p) {
  page.value = p || page.value
  loading.value = true
  try {
    const data = await listBooks({
      categoryId: effectiveCategoryId.value || undefined,
      page: page.value,
      size
    })
    books.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await categoryStore.ensureLoaded()
  load(1)
})
</script>

<style scoped>
.categories-page {
  max-width: 1080px;
  margin: 0 auto;
}
.root-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.child-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.child-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}
.page-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 18px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 200px);
  gap: 20px;
  min-height: 200px;
}
.pager {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
