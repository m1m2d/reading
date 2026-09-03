<template>
  <div class="home">
    <div class="section-head">
      <h2>{{ currentTitle }}</h2>
      <el-radio-group v-model="query.sort" @change="load">
        <el-radio-button value="newest">最新</el-radio-button>
        <el-radio-button value="downloads">热门</el-radio-button>
        <el-radio-button value="title">书名</el-radio-button>
      </el-radio-group>
    </div>
    <div v-loading="loading" class="grid" v-if="books.length">
      <BookCard v-for="book in books" :key="book.id" :book="book" />
    </div>
    <el-empty v-else-if="!loading" description="暂无书籍" />
    <div class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import BookCard from '../components/BookCard.vue'
import { listBooks } from '../api/books'
import { useCategoryStore } from '../store/categories'

const route = useRoute()
const categoryStore = useCategoryStore()
const books = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', categoryId: null, sort: 'newest', page: 1, size: 12 })

async function load() {
  loading.value = true
  try {
    const data = await listBooks({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      sort: query.sort,
      page: query.page,
      size: query.size
    })
    books.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onPage(page) {
  query.page = page
  load()
}

const currentTitle = computed(() => {
  if (query.keyword) return `“${query.keyword}” 的搜索结果`
  const name = categoryStore.findName(query.categoryId)
  return name ? `分类：${name}` : '全部书籍'
})

watch(
  () => route.query,
  (q) => {
    query.keyword = q.keyword || ''
    query.categoryId = q.categoryId ? Number(q.categoryId) : null
    query.page = 1
    load()
  },
  { immediate: true }
)

onMounted(() => {
  categoryStore.ensureLoaded()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.section-head h2 {
  font-size: 20px;
  color: #303133;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 200px);
  gap: 20px;
  min-height: 200px;
}
.pager {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}
</style>
