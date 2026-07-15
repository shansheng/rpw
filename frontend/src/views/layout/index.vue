<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <div class="logo">
        <h2 v-show="!isCollapse">资源计划预警</h2>
        <h2 v-show="isCollapse">RPW</h2>
      </div>
      <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
      >
        <el-menu-item index="/organization">
          <el-icon><OfficeBuilding /></el-icon>
          <span>组织架构</span>
        </el-menu-item>
        <el-menu-item index="/company">
          <el-icon><HomeFilled /></el-icon>
          <span>公司管理</span>
        </el-menu-item>
        <el-menu-item index="/project">
          <el-icon><FolderOpened /></el-icon>
          <span>项目管理</span>
        </el-menu-item>

        <el-sub-menu index="/resource-plan">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>资源计划</span>
          </template>
          <el-menu-item index="/resource-plan/material">材料计划</el-menu-item>
          <el-menu-item index="/resource-plan/equipment">设备计划</el-menu-item>
          <el-menu-item index="/resource-plan/hardware">五金计划</el-menu-item>
          <el-menu-item index="/resource-plan/circulation">周转材计划</el-menu-item>
          <el-menu-item index="/resource-plan/office">办公用品计划</el-menu-item>
          <el-menu-item index="/resource-plan/safety">安全物资计划</el-menu-item>
          <el-menu-item index="/resource-plan/subcontract">分包计划</el-menu-item>
          <el-menu-item index="/resource-plan/labor">劳动力计划</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/warning">
          <template #title>
            <el-icon><WarningFilled /></el-icon>
            <span>预警管理</span>
          </template>
          <el-menu-item index="/warning/board">预警看板</el-menu-item>
          <el-menu-item index="/warning/rule">规则配置</el-menu-item>
          <el-menu-item index="/warning/history">预警历史</el-menu-item>
          <el-menu-item index="/warning/statistics">预警统计</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/flowable">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>流程审批</span>
          </template>
          <el-menu-item index="/flowable/task">待办任务</el-menu-item>
          <el-menu-item index="/flowable/my-initiated">我发起的</el-menu-item>
          <el-menu-item index="/flowable/history">审批历史</el-menu-item>
          <el-menu-item index="/flowable/designer">流程设计器</el-menu-item>
          <el-menu-item index="/flowable/admin">流程管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/kanban">
          <el-icon><DataBoard /></el-icon>
          <span>看板</span>
        </el-menu-item>

        <el-sub-menu index="/report">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>报表</span>
          </template>
          <el-menu-item index="/report/config">自定义报表</el-menu-item>
          <el-menu-item index="/report/preview">报表预览</el-menu-item>
          <el-menu-item index="/report/export">导出中心</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/wecom/config">
          <el-icon><ChatLineSquare /></el-icon>
          <span>企业微信</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <el-icon style="cursor: pointer" @click="toggleCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="user-info">{{ username }}</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
const username = ref(localStorage.getItem('username') || '用户')

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  return (route.meta?.title as string) || ''
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}
.app-aside {
  background-color: #304156;
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo h2 {
  font-size: 16px;
  white-space: nowrap;
}
.app-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  font-size: 14px;
  color: #333;
}
.breadcrumb {
  font-size: 14px;
}
.app-main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 20px;
}
.el-menu {
  border-right: none;
}
</style>
