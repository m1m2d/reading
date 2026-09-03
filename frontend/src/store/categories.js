import { defineStore } from 'pinia'
import { getCategories } from '../api/admin'

export const useCategoryStore = defineStore('categories', {
  state: () => ({
    tree: [],
    loaded: false
  }),
  actions: {
    async ensureLoaded() {
      if (this.loaded) return
      this.tree = await getCategories()
      this.loaded = true
    },
    findName(id) {
      const walk = (nodes) => {
        for (const node of nodes) {
          if (node.id === Number(id)) return node.name
          const child = walk(node.children || [])
          if (child) return child
        }
        return null
      }
      return walk(this.tree)
    }
  }
})
