<template>
  <div class="wecom-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header-content">
        <div class="page-icon"><WechatOutlined /></div>
        <div>
          <div class="page-title">企业微信集成</div>
          <div class="page-desc">配置企业微信应用，实现预警消息推送和流程审批通知</div>
        </div>
      </div>
      <div class="header-actions">
        <a-tag :color="connectionStatus === 'connected' ? 'success' : connectionStatus === 'error' ? 'error' : 'default'">
          <template #icon>
            <CheckCircleOutlined v-if="connectionStatus === 'connected'" />
            <CloseCircleOutlined v-else-if="connectionStatus === 'error'" />
            <MinusCircleOutlined v-else />
          </template>
          {{ connectionStatus === 'connected' ? '已连接' : connectionStatus === 'error' ? '连接失败' : '未配置' }}
        </a-tag>
      </div>
    </div>

    <a-row :gutter="[16, 16]">
      <!-- 基础配置 -->
      <a-col :xs="24" :lg="16">
        <a-card class="config-card" title="应用配置">
          <template #extra>
            <a-button type="primary" :loading="saving" @click="handleSave" :icon="h(SaveOutlined)">
              保存配置
            </a-button>
          </template>

          <a-form
            ref="formRef"
            :model="form"
            :rules="rules"
            layout="vertical"
            class="config-form"
          >
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item label="企业 ID (CorpID)" name="corpId">
                  <a-input
                    v-model:value="form.corpId"
                    placeholder="请输入企业 CorpID"
                    :maxlength="50"
                    show-count
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item label="企业名称" name="corpName">
                  <a-input
                    v-model:value="form.corpName"
                    placeholder="请输入企业名称"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item label="应用 AgentID" name="agentId">
                  <a-input
                    v-model:value="form.agentId"
                    placeholder="请输入应用 AgentID"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item label="应用 Secret" name="agentSecret">
                  <a-input-password
                    v-model:value="form.agentSecret"
                    placeholder="请输入应用 Secret"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>

            <a-divider>消息回调（可选）</a-divider>

            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item label="Token（消息验证）">
                  <a-input v-model:value="form.token" placeholder="用于消息签名验证" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item label="EncodingAESKey">
                  <a-input-password v-model:value="form.encodingAesKey" placeholder="消息加解密密钥" allow-clear />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="Webhook URL（群机器人，可选）">
              <a-input v-model:value="form.webhookUrl" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." allow-clear />
            </a-form-item>

            <a-divider>通知设置</a-divider>

            <a-form-item label="启用消息通知">
              <a-switch v-model:checked="form.enableNotify" checked-children="开启" un-checked-children="关闭" />
            </a-form-item>

            <a-form-item label="通知类型" v-if="form.enableNotify">
              <a-checkbox-group v-model:value="form.notifyTypes" class="notify-checkboxes">
                <a-row :gutter="[8, 8]">
                  <a-col :span="12" v-for="nt in notifyTypeOptions" :key="nt.value">
                    <a-checkbox :value="nt.value">{{ nt.label }}</a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <!-- 右侧状态和帮助 -->
      <a-col :xs="24" :lg="8">
        <!-- 连接测试 -->
        <a-card class="config-card" title="连接测试" style="margin-bottom:16px">
          <div class="test-area">
            <div class="test-icon" :class="connectionStatus">
              <CheckCircleOutlined v-if="connectionStatus === 'connected'" />
              <CloseCircleOutlined v-else-if="connectionStatus === 'error'" />
              <ApiOutlined v-else />
            </div>
            <div class="test-status-text">
              <template v-if="connectionStatus === 'connected'">已成功连接企业微信</template>
              <template v-else-if="connectionStatus === 'error'">连接失败：{{ testMessage }}</template>
              <template v-else>点击测试按钮验证配置</template>
            </div>
            <a-button
              block
              :loading="testing"
              @click="handleTest"
              :type="connectionStatus === 'connected' ? 'default' : 'primary'"
              :icon="h(ThunderboltOutlined)"
              style="margin-top:12px"
            >
              {{ testing ? '测试中...' : '测试连接' }}
            </a-button>
          </div>
        </a-card>

        <!-- 回调地址 -->
        <a-card class="config-card" title="回调地址" style="margin-bottom:16px">
          <div class="callback-info">
            <div class="callback-label">系统回调 URL：</div>
            <div class="callback-url">
              <a-typography-paragraph :copyable="{ text: callbackUrl }" style="margin:0">
                <code class="callback-code">{{ callbackUrl || '保存配置后生成' }}</code>
              </a-typography-paragraph>
            </div>
            <div class="callback-tip">
              <InfoCircleOutlined style="margin-right:4px" />
              请将此地址配置到企业微信应用的「企业可信IP」和「接收消息 URL」
            </div>
          </div>
        </a-card>

        <!-- 接入说明 -->
        <a-card class="config-card" title="接入说明">
          <a-steps direction="vertical" size="small" :current="setupStep" class="setup-steps">
            <a-step title="创建企业应用">
              <template #description>
                登录企业微信管理后台，在「应用管理 → 应用」中创建自建应用
              </template>
            </a-step>
            <a-step title="填写应用信息">
              <template #description>
                复制 CorpID、AgentID 和 Secret 到左侧配置表单
              </template>
            </a-step>
            <a-step title="配置回调">
              <template #description>
                将回调地址填入应用的「接收消息 URL」，并填写 Token 和 AES Key
              </template>
            </a-step>
            <a-step title="测试连接">
              <template #description>
                点击「测试连接」验证配置是否正确
              </template>
            </a-step>
          </a-steps>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue'
import { message } from 'ant-design-vue'
import {
  WechatOutlined, SaveOutlined, CheckCircleOutlined, CloseCircleOutlined,
  MinusCircleOutlined, ApiOutlined, ThunderboltOutlined, InfoCircleOutlined
} from '@ant-design/icons-vue'
import { getWecomConfig, saveWecomConfig, testWecomConnection, getCallbackUrl } from '@/api/wecom'

const formRef = ref()
const saving = ref(false)
const testing = ref(false)
const connectionStatus = ref<'unknown' | 'connected' | 'error'>('unknown')
const testMessage = ref('')
const callbackUrl = ref('')

const form = reactive({
  corpId: '',
  corpName: '',
  agentId: '',
  agentSecret: '',
  token: '',
  encodingAesKey: '',
  webhookUrl: '',
  enableNotify: true,
  notifyTypes: ['WARNING', 'PROCESS_TASK'],
})

const rules = {
  corpId: [{ required: true, message: '请输入企业 CorpID', trigger: 'blur' }],
  agentId: [{ required: true, message: '请输入应用 AgentID', trigger: 'blur' }],
  agentSecret: [{ required: true, message: '请输入应用 Secret', trigger: 'blur' }],
}

const notifyTypeOptions = [
  { value: 'WARNING_CRITICAL', label: '严重预警' },
  { value: 'WARNING', label: '一般预警' },
  { value: 'PROCESS_TASK', label: '流程待办' },
  { value: 'PROCESS_RESULT', label: '审批结果' },
  { value: 'SYSTEM', label: '系统通知' },
]

const setupStep = computed(() => {
  if (!form.corpId || !form.agentId) return 0
  if (!form.agentSecret) return 1
  if (!callbackUrl.value) return 2
  if (connectionStatus.value !== 'connected') return 3
  return 4
})

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await saveWecomConfig(form)
    message.success('配置已保存')
    // 保存成功后尝试获取回调 URL
    fetchCallbackUrl()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  }
  saving.value = false
}

const handleTest = async () => {
  if (!form.corpId || !form.agentSecret) {
    message.warning('请先填写并保存企业 ID 和应用 Secret')
    return
  }
  testing.value = true
  try {
    const res = await testWecomConnection()
    if (res.data?.success) {
      connectionStatus.value = 'connected'
      message.success('连接成功！')
    } else {
      connectionStatus.value = 'error'
      testMessage.value = res.data?.message || '未知错误'
      message.error('连接失败：' + testMessage.value)
    }
  } catch (e: any) {
    connectionStatus.value = 'error'
    testMessage.value = e?.message || '请求失败'
    message.error('连接测试请求失败')
  }
  testing.value = false
}

const fetchCallbackUrl = async () => {
  try {
    const res = await getCallbackUrl()
    callbackUrl.value = res.data?.url || window.location.origin + '/api/v1/wecom/callback'
  } catch {
    callbackUrl.value = window.location.origin + '/api/v1/wecom/callback'
  }
}

onMounted(async () => {
  // 加载已保存的配置
  try {
    const res = await getWecomConfig()
    if (res.data) {
      Object.assign(form, res.data)
      if (form.corpId && form.agentSecret) {
        connectionStatus.value = 'unknown'
      }
    }
  } catch {
    // 还没有配置，使用默认值
  }
  fetchCallbackUrl()
})
</script>

<style scoped>
.wecom-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.page-header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #07c160, #06ad56);
  font-size: 24px;
  color: #fff;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.85);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}

/* ===== 配置卡片 ===== */
.config-card {
  border-radius: 12px !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06) !important;
  border: none !important;
}

.config-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
}

.notify-checkboxes :deep(.ant-checkbox-wrapper) {
  margin: 0;
}

/* ===== 连接测试 ===== */
.test-area {
  text-align: center;
  padding: 8px 0;
}

.test-icon {
  font-size: 40px;
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.25);
}

.test-icon.connected {
  color: #52c41a;
}

.test-icon.error {
  color: #ff4d4f;
}

.test-status-text {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  min-height: 20px;
}

/* ===== 回调地址 ===== */
.callback-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.callback-label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}

.callback-code {
  font-size: 12px;
  word-break: break-all;
  color: #1890ff;
}

.callback-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  background: #fffbe6;
  padding: 8px 10px;
  border-radius: 6px;
  border-left: 3px solid #faad14;
}

/* ===== 步骤说明 ===== */
.setup-steps :deep(.ant-steps-item-description) {
  font-size: 12px !important;
  color: rgba(0, 0, 0, 0.45) !important;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
