<template>
  <div class="login-wrapper">
    <div class="login-bg-pattern"></div>
    <div class="login-bg-circle c1"></div>
    <div class="login-bg-circle c2"></div>
    <div class="login-bg-circle c3"></div>

    <div class="login-card">
      <!-- 品牌区域 -->
      <div class="brand">
        <div class="brand-icon">
          <AuditOutlined />
        </div>
        <h1 class="brand-title">资源计划预警系统</h1>
        <p class="brand-subtitle">Resource Plan &amp; Warning System</p>
      </div>

      <!-- 登录表单 -->
      <a-form layout="vertical" @submit.prevent="handleLogin" class="login-form">
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="username"
            placeholder="请输入用户名"
            size="large"
            autocomplete="username"
          >
            <template #prefix><UserOutlined style="color:#bfbfbf" /></template>
          </a-input>
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="password"
            placeholder="请输入密码"
            size="large"
            autocomplete="current-password"
          >
            <template #prefix><LockOutlined style="color:#bfbfbf" /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" block size="large" :loading="loading" class="login-btn">
            {{ loading ? '登录中...' : '登 录' }}
          </a-button>
        </a-form-item>

        <div class="login-footer">
          <span>默认账号: admin / admin123</span>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, AuditOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

const handleLogin = async () => {
  if (!username.value || !password.value) {
    message.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: username.value, password: password.value })
    message.success('登录成功')
    router.push('/dashboard')
  } catch (err: any) {
    message.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 50%, #003a8c 100%);
  overflow: hidden;
}

/* Decorative bg elements */
.login-bg-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 25% 25%, rgba(255,255,255,0.05) 0%, transparent 50%),
                    radial-gradient(circle at 75% 75%, rgba(255,255,255,0.05) 0%, transparent 50%);
}
.login-bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: #fff;
}
.c1 { width: 600px; height: 600px; top: -200px; left: -200px; }
.c2 { width: 400px; height: 400px; bottom: -100px; right: -100px; }
.c3 { width: 300px; height: 300px; top: 40%; right: 15%; }

.login-card {
  position: relative;
  width: 420px;
  padding: 48px 40px 36px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: cardIn 0.6s ease both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.brand {
  text-align: center;
  margin-bottom: 36px;
}
.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  border-radius: 14px;
  margin-bottom: 16px;
  font-size: 28px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
}
.brand-title {
  font-size: 22px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}
.brand-subtitle {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.4);
  letter-spacing: 1px;
}

.login-form :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}
.login-form :deep(.ant-input-affix-wrapper) {
  padding: 8px 12px;
  border-radius: 8px;
  border-color: #e8e8e8;
  transition: all 0.3s;
}
.login-form :deep(.ant-input-affix-wrapper:hover),
.login-form :deep(.ant-input-affix-wrapper-focused) {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.login-btn {
  height: 44px;
  font-size: 15px;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  border: none;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.35);
  transition: all 0.3s;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.45);
}

.login-footer {
  text-align: center;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.3);
  margin-top: 8px;
}
</style>
