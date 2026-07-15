import router from './index'
import { getToken } from '@/utils/auth'

/**
 * 全局路由守卫
 * 功能：
 * 1. 判断目标路由是否需要认证
 * 2. 未登录时重定向到登录页
 * 3. 已登录时访问登录页则重定向到首页
 */
router.beforeEach((to, from) => {
  // 设置页面标题
  const title = to.meta.title as string
  document.title = title ? `${title} - 资源计划预警系统` : '资源计划预警系统'

  // 判断路由是否需要认证
  const requiresAuth = to.meta.requiresAuth !== false
  const token = getToken()

  if (requiresAuth) {
    // 需要认证，检查Token
    if (token) {
      return true
    } else {
      // 未登录，重定向到登录页
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  } else {
    // 不需要认证（如登录页）
    if (token && to.path === '/login') {
      // 已登录访问登录页，重定向到首页
      return '/dict'
    } else {
      return true
    }
  }
})

export default router
