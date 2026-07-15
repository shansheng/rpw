<template>
  <a-layout class="app-layout">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobile && !collapsed"
      class="mobile-overlay"
      @click="collapsed = true"
    />

    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      theme="dark"
      :width="240"
      :collapsed-width="isMobile ? 0 : 72"
      :class="['app-sider', { 'mobile-sider': isMobile }]"
      :style="isMobile ? { position: 'fixed', zIndex: 200, height: '100vh' } : {}"
    >
      <div class="sider-header" :class="{ collapsed: collapsed && !isMobile }">
        <div class="logo-box">
          <AuditOutlined class="logo-icon" />
          <span v-show="!collapsed || isMobile" class="logo-text">资源计划预警</span>
        </div>
        <!-- 移动端关闭按钮 -->
        <CloseOutlined
          v-if="isMobile"
          class="mobile-close-btn"
          @click="collapsed = true"
        />
      </div>

      <a-menu
        theme="dark"
        mode="inline"
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        class="sider-menu"
      >
        <template v-for="item in menuItems" :key="item.key">
          <a-sub-menu v-if="item.children" :key="item.key">
            <template #title>
              <component :is="item.icon" />
              <span>{{ item.label }}</span>
            </template>
            <a-menu-item
              v-for="child in item.children"
              :key="child.key"
              @click="handleMenuSelect(child.key)"
            >
              <span>{{ child.label }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="item.key" @click="handleMenuSelect(item.key)">
            <component :is="item.icon" />
            <span>{{ item.label }}</span>
          </a-menu-item>
        </template>
      </a-menu>

      <!-- PC 端折叠按钮 -->
      <div v-if="!isMobile" class="sider-footer" @click="toggleCollapsed">
        <component :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined" />
        <span v-show="!collapsed">收起侧边栏</span>
      </div>
    </a-layout-sider>

    <!-- 右侧区域 -->
    <a-layout :class="['right-layout', { 'mobile-layout': isMobile }]" :style="rightLayoutStyle">
      <!-- 顶部 Header -->
      <a-layout-header class="app-header">
        <div class="header-left">
          <!-- 移动端 hamburger 按钮 -->
          <a-button
            v-if="isMobile"
            type="text"
            class="hamburger-btn"
            @click="collapsed = false"
          >
            <MenuOutlined style="font-size:18px" />
          </a-button>
          <!-- PC 端折叠按钮 -->
          <a-button
            v-else
            type="text"
            class="hamburger-btn"
            @click="toggleCollapsed"
          >
            <component :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined" style="font-size:18px" />
          </a-button>
          <a-breadcrumb class="app-breadcrumb" v-if="!isMobile">
            <a-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path">
              {{ item.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
          <span v-else class="mobile-page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="header-right">
          <a-tooltip title="消息通知">
            <a-badge :count="unreadCount" :overflow-count="99" class="notif-badge">
              <BellOutlined
                class="header-icon"
                @click="showNotifications = !showNotifications"
              />
            </a-badge>
          </a-tooltip>
          <a-dropdown placement="bottomRight">
            <div class="user-dropdown">
              <a-avatar size="small" style="background-color:#1890ff;">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span class="user-name" v-if="!isMobile">{{ userStore.displayName || '用户' }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="username" disabled>
                  <span style="color:#999">{{ userStore.displayName || '用户' }}</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="profile"><UserOutlined /> 个人中心</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout" danger>
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区域 -->
      <a-layout-content class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  DashboardOutlined, ApartmentOutlined, ScheduleOutlined,
  AlertOutlined, AuditOutlined, AppstoreOutlined,
  FileTextOutlined, WechatOutlined, ProjectOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, UserOutlined, LogoutOutlined,
  BellOutlined, MenuOutlined, CloseOutlined, BookOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 响应式状态
const collapsed = ref(false)
const isMobile = ref(false)
const openKeys = ref<string[]>([])
const showNotifications = ref(false)
const unreadCount = ref(3)

// 检测是否是移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    collapsed.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  // 初始化展开当前路由所在的菜单组
  updateOpenKeys()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

// 右侧布局样式
const rightLayoutStyle = computed(() => {
  if (isMobile.value) return {}
  return {
    marginLeft: collapsed.value ? '72px' : '240px',
    transition: 'margin-left 0.2s ease'
  }
})

const toggleCollapsed = () => { collapsed.value = !collapsed.value }

const breadcrumbItems = computed(() =>
  route.matched.filter(r => r.meta?.title).map(r => ({
    path: r.path,
    title: (r.meta?.title as string) || ''
  }))
)

const currentPageTitle = computed(() => {
  const last = breadcrumbItems.value[breadcrumbItems.value.length - 1]
  return last?.title || '工作台'
})

const selectedKeys = computed(() => [route.path])

const updateOpenKeys = () => {
  // 根据当前路由找出应该展开的菜单组
  const path = route.path
  for (const item of menuItems) {
    if (item.children) {
      const found = item.children.find(c => c.key === path)
      if (found && !openKeys.value.includes(item.key)) {
        openKeys.value = [...openKeys.value, item.key]
      }
    }
  }
}

watch(() => route.path, () => {
  updateOpenKeys()
  // 移动端跳转后自动关闭菜单
  if (isMobile.value) {
    collapsed.value = true
  }
})

const handleMenuSelect = (key: string) => {
  router.push(key)
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

interface MenuItem { key: string; label: string; icon?: any; children?: MenuItem[] }

const menuItems: MenuItem[] = [
  { key: '/dashboard', label: '工作台', icon: DashboardOutlined },
  { key: 'basic', label: '基础管理', icon: ApartmentOutlined, children: [
    { key: '/organization', label: '组织架构' },
    { key: '/company', label: '公司管理' },
    { key: '/project', label: '项目管理' },
    { key: '/dict', label: '字典管理' }
  ]},
  { key: 'resource', label: '资源计划', icon: ScheduleOutlined, children: [
    { key: '/resource-plan/material', label: '材料计划' },
    { key: '/resource-plan/equipment', label: '设备计划' },
    { key: '/resource-plan/safety', label: '安全物资计划' },
    { key: '/resource-plan/office', label: '办公用品计划' },
    { key: '/resource-plan/hardware', label: '五金计划' },
    { key: '/resource-plan/circulation', label: '周转材计划' },
    { key: '/resource-plan/subcontract', label: '分包计划' },
    { key: '/resource-plan/labor', label: '劳动力计划' }
  ]},
  { key: 'warning', label: '预警管理', icon: AlertOutlined, children: [
    { key: '/warning/board', label: '预警看板' },
    { key: '/warning/rule', label: '预警规则' },
    { key: '/warning/history', label: '预警历史' },
    { key: '/warning/statistics', label: '预警统计' }
  ]},
  { key: 'flowable', label: '流程审批', icon: AuditOutlined, children: [
    { key: '/flowable/task', label: '待办任务' },
    { key: '/flowable/my-initiated', label: '我发起的' },
    { key: '/flowable/history', label: '审批历史' },
    { key: '/flowable/admin', label: '流程管理' },
    { key: '/flowable/designer', label: '流程设计' }
  ]},
  { key: '/kanban', label: '看板', icon: AppstoreOutlined },
  { key: 'report', label: '报表管理', icon: FileTextOutlined, children: [
    { key: '/report/config', label: '报表配置' },
    { key: '/report/preview', label: '报表预览' },
    { key: '/report/export', label: '报表导出' }
  ]},
  { key: '/wecom/config', label: '企业微信', icon: WechatOutlined },
]
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

/* ===== 遮罩层 ===== */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 199;
}

/* ===== 侧边栏 ===== */
.app-sider {
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  z-index: 100;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  transition: width 0.2s ease, left 0.2s ease;
}

.mobile-sider {
  z-index: 200 !important;
  /* 移动端折叠时隐藏到左侧 */
  left: 0;
}

.sider-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.sider-header.collapsed {
  justify-content: center;
  padding: 0;
}

.logo-box {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.logo-icon {
  font-size: 24px;
  color: #1890ff;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
}

.mobile-close-btn {
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
  flex-shrink: 0;
}

.mobile-close-btn:hover {
  color: #fff;
}

.sider-menu {
  border-right: none !important;
  padding: 8px 0;
  overflow-y: auto;
  height: calc(100vh - 64px - 48px);
}

.sider-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: 13px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  transition: color 0.2s;
}

.sider-footer:hover {
  color: #fff;
}

/* ===== 右侧布局 ===== */
.right-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

.mobile-layout {
  margin-left: 0 !important;
}

/* ===== 顶部 Header ===== */
.app-header {
  position: sticky;
  top: 0;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 16px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  line-height: normal;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.hamburger-btn {
  color: rgba(0, 0, 0, 0.65);
  flex-shrink: 0;
}

.hamburger-btn:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.06);
}

.app-breadcrumb {
  font-size: 14px;
}

.mobile-page-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.header-icon {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  transition: color 0.2s;
}

.header-icon:hover {
  color: #1890ff;
}

.notif-badge :deep(.ant-badge-count) {
  font-size: 11px;
  box-shadow: none;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-dropdown:hover {
  background: rgba(0, 0, 0, 0.04);
}

.user-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 内容区域 ===== */
.app-content {
  margin: 16px;
  min-height: calc(100vh - 64px - 32px);
}

@media (min-width: 768px) {
  .app-content {
    margin: 20px 24px;
  }
}

/* ===== 页面切换动画 ===== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ===== 菜单样式优化 ===== */
:deep(.ant-menu-dark .ant-menu-item-selected) {
  background-color: #1890ff !important;
  border-radius: 6px;
}

:deep(.ant-menu-dark .ant-menu-item:hover) {
  border-radius: 6px;
}

:deep(.ant-menu-dark.ant-menu-inline .ant-menu-item) {
  border-radius: 6px;
  margin: 2px 8px;
  width: calc(100% - 16px);
}

:deep(.ant-menu-dark.ant-menu-inline .ant-menu-submenu-title) {
  border-radius: 6px;
  margin: 2px 8px;
  width: calc(100% - 16px);
}
</style>
