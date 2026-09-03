import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/ClientLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue'), meta: { public: true } },
      { path: 'categories', name: 'categories', component: () => import('../views/CategoriesView.vue'), meta: { public: true } },
      { path: 'discussion', name: 'discussion', component: () => import('../views/DiscussionView.vue'), meta: { public: true } },
      { path: 'post/:id', name: 'post-detail', component: () => import('../views/PostDetailView.vue'), meta: { public: true } },
      { path: 'book/:id', name: 'book-detail', component: () => import('../views/BookDetailView.vue'), meta: { public: true } },
      { path: 'user/:id', name: 'user-profile', component: () => import('../views/UserProfileView.vue'), meta: { public: true } },
      { path: 'reader/:id', name: 'reader', component: () => import('../views/ReaderView.vue'), meta: { public: true } },
      { path: 'upload', name: 'upload', component: () => import('../views/UploadView.vue'), meta: { auth: true } },
      { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
      { path: 'following', name: 'following', component: () => import('../views/MyFollowingView.vue'), meta: { auth: true } }
    ]
  },
  { path: '/favorites', redirect: '/profile?tab=favorites' },
  { path: '/my-uploads', redirect: '/profile?tab=uploads' },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboard.vue') },
      { path: 'monitor', name: 'admin-monitor', component: () => import('../views/admin/MonitorCenter.vue') },
      { path: 'books', name: 'admin-books', component: () => import('../views/admin/BookAudit.vue') },
      { path: 'categories', name: 'admin-categories', component: () => import('../views/admin/CategoryManage.vue') },
      { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/CommentManage.vue') },
      { path: 'users', name: 'admin-users', component: () => import('../views/admin/UserManage.vue') },
      { path: 'config', name: 'admin-config', component: () => import('../views/admin/ConfigManage.vue') },
      { path: 'user-requests', name: 'admin-user-requests', component: () => import('../views/admin/UserRequestsView.vue') },
      { path: 'user-request-logs', name: 'admin-user-request-logs', component: () => import('../views/admin/UserRequestsView.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !auth.isAdmin) {
    return { path: '/' }
  }
  return true
})

export default router
