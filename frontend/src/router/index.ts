import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

/**
 * 路由配置
 * - /login: 登录页（无需认证，无Layout）
 * - /: 使用Layout包裹所有需要认证的页面
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '',
    component: () => import('@/views/layout/index.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '/dict', name: 'Dict', component: () => import('@/views/dict/index.vue'), meta: { title: '字典管理', requiresAuth: true } },
      { path: '/organization', name: 'Organization', component: () => import('@/views/organization/index.vue'), meta: { title: '组织架构', requiresAuth: true } },
      { path: '/company', name: 'Company', component: () => import('@/views/company/index.vue'), meta: { title: '公司管理', requiresAuth: true } },
      { path: '/project', name: 'Project', component: () => import('@/views/project/index.vue'), meta: { title: '项目管理', requiresAuth: true } },
      { path: '/kanban', name: 'Kanban', component: () => import('@/views/kanban/index.vue'), meta: { title: '看板', requiresAuth: true } },
      // 资源计划
      { path: '/resource-plan/material', name: 'ResourcePlanMaterial', component: () => import('@/views/resource-plan/material/index.vue'), meta: { title: '材料计划', requiresAuth: true } },
      { path: '/resource-plan/equipment', name: 'ResourcePlanEquipment', component: () => import('@/views/resource-plan/equipment/index.vue'), meta: { title: '设备计划', requiresAuth: true } },
      { path: '/resource-plan/hardware', name: 'ResourcePlanHardware', component: () => import('@/views/resource-plan/hardware/index.vue'), meta: { title: '五金计划', requiresAuth: true } },
      { path: '/resource-plan/office', name: 'ResourcePlanOffice', component: () => import('@/views/resource-plan/office/index.vue'), meta: { title: '办公用品计划', requiresAuth: true } },
      { path: '/resource-plan/safety', name: 'ResourcePlanSafety', component: () => import('@/views/resource-plan/safety/index.vue'), meta: { title: '安全物资计划', requiresAuth: true } },
      { path: '/resource-plan/circulation', name: 'ResourcePlanCirculation', component: () => import('@/views/resource-plan/circulation/index.vue'), meta: { title: '周转材计划', requiresAuth: true } },
      { path: '/resource-plan/subcontract', name: 'ResourcePlanSubcontract', component: () => import('@/views/resource-plan/subcontract/index.vue'), meta: { title: '分包计划管理', requiresAuth: true } },
      { path: '/resource-plan/labor', name: 'ResourcePlanLabor', component: () => import('@/views/resource-plan/labor/index.vue'), meta: { title: '劳动力计划管理', requiresAuth: true } },
      // 预警
      { path: '/warning/board', name: 'WarningBoard', component: () => import('@/views/warning/board/index.vue'), meta: { title: '预警看板', requiresAuth: true } },
      { path: '/warning/rule', name: 'WarningRule', component: () => import('@/views/warning/rule/index.vue'), meta: { title: '预警规则配置', requiresAuth: true } },
      { path: '/warning/history', name: 'WarningHistory', component: () => import('@/views/warning/history/index.vue'), meta: { title: '预警历史', requiresAuth: true } },
      { path: '/warning/statistics', name: 'WarningStatistics', component: () => import('@/views/warning/statistics/index.vue'), meta: { title: '预警统计', requiresAuth: true } },
      // 流程审批
      { path: '/flowable/task', name: 'FlowableTask', component: () => import('@/views/flowable/task/index.vue'), meta: { title: '待办任务', requiresAuth: true } },
      { path: '/flowable/my-initiated', name: 'FlowableMyInitiated', component: () => import('@/views/flowable/my-initiated/index.vue'), meta: { title: '我发起的审批', requiresAuth: true } },
      { path: '/flowable/history', name: 'FlowableHistory', component: () => import('@/views/flowable/history/index.vue'), meta: { title: '审批历史', requiresAuth: true } },
      { path: '/flowable/admin', name: 'FlowableAdmin', component: () => import('@/views/flowable/admin/index.vue'), meta: { title: '流程管理', requiresAuth: true } },
      { path: '/flowable/designer', name: 'FlowableDesigner', component: () => import('@/views/flowable/designer/index.vue'), meta: { title: '流程设计器', requiresAuth: true } },
      // 报表
      { path: '/report/config', name: 'ReportConfig', component: () => import('@/views/report/config.vue'), meta: { title: '自定义报表', requiresAuth: true } },
      { path: '/report/preview', name: 'ReportPreview', component: () => import('@/views/report/preview.vue'), meta: { title: '报表预览', requiresAuth: true } },
      { path: '/report/export', name: 'ReportExport', component: () => import('@/views/report/export.vue'), meta: { title: '导出中心', requiresAuth: true } },
      // 企业微信
      { path: '/wecom/config', name: 'WeComConfig', component: () => import('@/views/wecom/config/index.vue'), meta: { title: '企业微信配置', requiresAuth: true } }
    ]
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
