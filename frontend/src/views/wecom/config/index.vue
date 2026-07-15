<template>
  <div class="wecom-config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>企业微信配置</span>
          <el-button type="primary" @click="handleTest">测试连接</el-button>
        </div>
      </template>

      <el-form
          ref="configFormRef"
          :model="configForm"
          :rules="configRules"
          label-width="150px"
          style="max-width: 600px"
      >
        <el-form-item label="企业ID (CorpId)" prop="corpId">
          <el-input v-model="configForm.corpId" placeholder="请输入企业ID" />
        </el-form-item>

        <el-form-item label="应用AgentId" prop="agentId">
          <el-input v-model="configForm.agentId" placeholder="请输入应用AgentId" />
        </el-form-item>

        <el-form-item label="应用Secret" prop="secret">
          <el-input
              v-model="configForm.secret"
              type="password"
              placeholder="请输入应用Secret"
              show-password
          />
        </el-form-item>

        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="configForm.apiUrl" placeholder="https://qyapi.weixin.qq.com" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
          <el-button @click="handleTest">测试连接</el-button>
        </el-form-item>
      </el-form>

      <!-- 连接状态 -->
      <el-divider />
      <div class="connection-status">
        <h4>连接状态</h4>
        <el-tag :type="connectionStatus.type">{{ connectionStatus.text }}</el-tag>
      </div>

      <!-- 消息发送测试 -->
      <el-divider />
      <div class="message-test">
        <h4>消息发送测试</h4>
        <el-form :inline="true">
          <el-form-item label="接收人">
            <el-input v-model="testUser" placeholder="@all 或用户名" style="width: 200px" />
          </el-form-item>
          <el-form-item label="消息内容">
            <el-input v-model="testMessage" placeholder="测试消息内容" style="width: 300px" />
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="handleSendTest">发送测试</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getWeComConfig, saveWeComConfig, testWeComConnection, sendTextMessage } from '@/api/wecom'

/**
 * 企业微信配置页面
 * - 配置企业微信参数
 * - 测试连接
 * - 发送测试消息
 */
const configFormRef = ref()
const testUser = ref('@all')
const testMessage = ref('这是一条测试消息')
const connectionStatus = ref<{ type: 'success' | 'danger' | 'info'; text: string }>({
  type: 'info',
  text: '未测试'
})

// 表单数据
const configForm = reactive({
  corpId: '',
  agentId: '',
  secret: '',
  apiUrl: 'https://qyapi.weixin.qq.com'
})

// 表单验证规则
const configRules = {
  corpId: [{ required: true, message: '请输入企业ID', trigger: 'blur' }],
  agentId: [{ required: true, message: '请输入应用AgentId', trigger: 'blur' }],
  secret: [{ required: true, message: '请输入应用Secret', trigger: 'blur' }]
}

// 获取配置
const fetchConfig = async () => {
  try {
    const res = await getWeComConfig()
    if (res.data.code === 200 && res.data.data) {
      Object.assign(configForm, res.data.data)
    }
  } catch (error) {
    console.error('获取配置失败', error)
  }
}

// 保存配置
const handleSave = () => {
  configFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      try {
        const res = await saveWeComConfig({ ...configForm })
        if (res.data.code === 200 && res.data.data) {
          ElMessage.success('保存成功')
        }
      } catch (error) {
        console.error('保存失败', error)
      }
    }
  })
}

// 测试连接
const handleTest = async () => {
  try {
    const res = await testWeComConnection()
    if (res.data.code === 200) {
      connectionStatus.value = { type: 'success', text: '连接成功' }
      ElMessage.success('连接成功')
    } else {
      connectionStatus.value = { type: 'danger', text: '连接失败' }
      ElMessage.error('连接失败')
    }
  } catch (error: any) {
    connectionStatus.value = { type: 'danger', text: '连接异常' }
    ElMessage.error('连接异常: ' + (error.message || ''))
  }
}

// 发送测试消息
const handleSendTest = async () => {
  if (!testMessage.value) {
    ElMessage.warning('请输入测试消息')
    return
  }

  try {
    const res = await sendTextMessage({
      touser: testUser.value,
      content: testMessage.value
    })
    if (res.data.code === 200 && res.data.data) {
      ElMessage.success('消息发送成功')
    } else {
      ElMessage.error('消息发送失败')
    }
  } catch (error) {
    console.error('消息发送异常', error)
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.wecom-config-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.connection-status, .message-test {
  margin-top: 20px;
}

.connection-status h4, .message-test h4 {
  margin-bottom: 10px;
  color: #303133;
}
</style>
