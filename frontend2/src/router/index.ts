import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台' }
      },
      // ---- 组织架构 ----
      {
        path: 'organization',
        name: 'Organization',
        component: () => import('@/views/organization/index.vue'),
        meta: { title: '组织架构' }
      },
      // ---- 公司管理 ----
      {
        path: 'company',
        name: 'Company',
        component: () => import('@/views/company/index.vue'),
        meta: { title: '公司管理' }
      },
      // ---- 项目管理 ----
      {
        path: 'project',
        name: 'Project',
        component: () => import('@/views/project/index.vue'),
        meta: { title: '项目管理' }
      },
      // ---- 资源计划（动态路由） ----
      {
        path: 'resource-plan/:type',
        name: 'ResourcePlan',
        component: () => import('@/views/resource-plan/index.vue'),
        meta: { title: '资源计划' }
      },
      // ---- 预警管理 ----
      {
        path: 'warning/board',
        name: 'WarningBoard',
        component: () => import('@/views/warning/board/index.vue'),
        meta: { title: '预警看板' }
      },
      {
        path: 'warning/rule',
        name: 'WarningRule',
        component: () => import('@/views/warning/rule/index.vue'),
        meta: { title: '预警规则' }
      },
      {
        path: 'warning/history',
        name: 'WarningHistory',
        component: () => import('@/views/warning/history/index.vue'),
        meta: { title: '预警历史' }
      },
      {
        path: 'warning/statistics',
        name: 'WarningStatistics',
        component: () => import('@/views/warning/statistics/index.vue'),
        meta: { title: '预警统计' }
      },
      // ---- 流程审批 ----
      {
        path: 'flowable/task',
        name: 'FlowableTask',
        component: () => import('@/views/flowable/task/index.vue'),
        meta: { title: '待办任务' }
      },
      {
        path: 'flowable/my-initiated',
        name: 'FlowableMyInitiated',
        component: () => import('@/views/flowable/my-initiated/index.vue'),
        meta: { title: '我发起的' }
      },
      {
        path: 'flowable/history',
        name: 'FlowableHistory',
        component: () => import('@/views/flowable/history/index.vue'),
        meta: { title: '审批历史' }
      },
      {
        path: 'flowable/admin',
        name: 'FlowableAdmin',
        component: () => import('@/views/flowable/admin/index.vue'),
        meta: { title: '流程管理' }
      },
      {
        path: 'flowable/designer',
        name: 'FlowableDesigner',
        component: () => import('@/views/flowable/designer/index.vue'),
        meta: { title: '流程设计' }
      },
      // ---- 看板 ----
      {
        path: 'kanban',
        name: 'Kanban',
        component: () => import('@/views/kanban/index.vue'),
        meta: { title: '看板' }
      },
      // ---- 报表 ----
      {
        path: 'report/config',
        name: 'ReportConfig',
        component: () => import('@/views/report/config/index.vue'),
        meta: { title: '报表配置' }
      },
      {
        path: 'report/preview',
        name: 'ReportPreview',
        component: () => import('@/views/report/preview/index.vue'),
        meta: { title: '报表预览' }
      },
      {
        path: 'report/export',
        name: 'ReportExport',
        component: () => import('@/views/report/export/index.vue'),
        meta: { title: '报表导出' }
      },
      // ---- 字典管理 ----
      {
        path: 'dict',
        name: 'Dict',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '字典管理' }
      },
      // ---- 企业微信 ----
      {
        path: 'wecom/config',
        name: 'WecomConfig',
        component: () => import('@/views/wecom/config/index.vue'),
        meta: { title: '企业微信配置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta?.noAuth) {
    // 无需登录的页面
    next()
  } else if (!token) {
    // 未登录，跳转登录页
    next('/login')
  } else {
    next()
  }
})

export default router
