<template>
  <div class="designer-wrapper">
    <!-- 移动端：顶部切换栏 -->
    <div v-if="isMobile" class="mobile-tabs">
      <div
        v-for="tab in mobileTabs"
        :key="tab.key"
        class="mobile-tab"
        :class="{ active: activeMobileTab === tab.key }"
        @click="activeMobileTab = tab.key"
      >
        <component :is="tab.icon" style="font-size: 16px;" />
        <span style="font-size: 12px; margin-top: 2px;">{{ tab.label }}</span>
      </div>
    </div>

    <a-row :gutter="isMobile ? 0 : 12" style="height: 100%;">
      <!-- 左侧：流程列表 -->
      <a-col
        :span="isMobile ? 24 : 4"
        :style="isMobile ? (activeMobileTab === 'list' ? {} : { display: 'none' }) : {}"
        style="height: 100%;"
      >
        <a-card
          title="流程定义"
          :bordered="true"
          class="h-full"
          :body-style="isMobile ? { padding: '8px', height: 'calc(100vh - 200px)', overflow: 'auto' } : {}"
        >
          <template #extra>
            <a-button type="primary" size="small" @click="handleCreateNew">新建</a-button>
          </template>
          <div class="process-list">
            <div v-if="loading" style="text-align:center;padding:20px;color:#999;">加载中...</div>
            <div v-else-if="processDefs.length === 0" style="text-align:center;padding:20px;color:#999;font-size:13px;">
              暂无流程定义
            </div>
            <div
              v-for="item in processDefs"
              :key="item.id"
              class="process-item"
              :class="{ active: currentDefId === item.id }"
              @click="handleSelect(item)"
            >
              <div class="process-name">{{ item.name || item.key }}</div>
              <div class="process-meta">
                <a-tag color="blue" style="font-size:11px;">v{{ item.version }}</a-tag>
                <span style="font-size:11px;color:#999;">{{ item.key }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 中间：画布 -->
      <a-col
        :span="isMobile ? 24 : 14"
        :style="isMobile ? (activeMobileTab === 'canvas' ? {} : { display: 'none' }) : {}"
        style="height: 100%;"
      >
        <a-card
          :bordered="true"
          class="h-full"
          :body-style="isMobile ? { padding: '4px', height: 'calc(100vh - 200px)' } : { padding: '0' }"
        >
          <template #title>
            <div class="toolbar" :class="{ 'toolbar--mobile': isMobile }">
              <a-space :size="isMobile ? 2 : 4">
                <a-button size="small" @click="undo" :disabled="!canUndo">撤销</a-button>
                <a-button size="small" @click="redo" :disabled="!canRedo">重做</a-button>
                <a-divider v-if="!isMobile" type="vertical" />
                <a-button v-if="!isMobile" size="small" @click="zoomIn">放大</a-button>
                <a-button v-if="!isMobile" size="small" @click="zoomOut">缩小</a-button>
                <a-button size="small" @click="zoomReset">适应</a-button>
                <a-divider v-if="!isMobile" type="vertical" />
                <a-button size="small" type="primary" @click="handleSave">保存</a-button>
                <a-button size="small" type="primary" @click="handleDeploy">部署</a-button>
                <a-button size="small" @click="handleExport">导出</a-button>
              </a-space>
              <a-tag v-if="!isMobile" color="geekblue">{{ processName }}</a-tag>
            </div>
          </template>
          <div
            id="canvas-container"
            :style="isMobile
              ? 'width:100%;height:calc(100vh - 260px);'
              : 'width:100%;height:calc(100vh - 260px);'"
          ></div>
        </a-card>
      </a-col>

      <!-- 右侧：属性面板 -->
      <a-col
        :span="isMobile ? 24 : 6"
        :style="isMobile ? (activeMobileTab === 'props' ? {} : { display: 'none' }) : {}"
        style="height: 100%;"
      >
        <a-card
          title="属性面板"
          :bordered="true"
          class="h-full"
          :body-style="isMobile ? { padding: '8px', height: 'calc(100vh - 200px)', overflow: 'auto' } : {}"
        >
          <div id="properties-panel" class="properties-panel"></div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
} from 'bpmn-js-properties-panel'
import camundaModdle from 'camunda-bpmn-moddle/resources/camunda.json'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

import { deployProcessDefinition } from '@/api/flowable'
import request from '@/api/request'
import {
  AppstoreOutlined,
  EditOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'

const isMobile = ref(false)
const activeMobileTab = ref('canvas')
const mobileTabs = [
  { key: 'list', label: '列表', icon: AppstoreOutlined },
  { key: 'canvas', label: '画布', icon: EditOutlined },
  { key: 'props', label: '属性', icon: SettingOutlined }
]

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value && !activeMobileTab.value) {
    activeMobileTab.value = 'canvas'
  }
}

const modeler = ref<any>(null)
const processName = ref('新流程')
const processKey = ref('process_' + Date.now())
const processDefs = ref<any[]>([])
const currentDefId = ref<string>('')
const loading = ref(false)
const canUndo = ref(false)
const canRedo = ref(false)

const defaultXml = () => `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
  targetNamespace="http://bpmn.io/schema/bpmn" id="def_1">
  <bpmn:process id="${processKey.value}" name="${processName.value}" isExecutable="true">
    <bpmn:startEvent id="start" name="开始"><bpmn:outgoing>flow1</bpmn:outgoing></bpmn:startEvent>
    <bpmn:sequenceFlow id="flow1" sourceRef="start" targetRef="end" />
    <bpmn:endEvent id="end" name="结束"><bpmn:incoming>flow1</bpmn:incoming></bpmn:endEvent>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="diag_1">
    <bpmndi:BPMNPlane id="plane_1" bpmnElement="${processKey.value}">
      <bpmndi:BPMNShape id="shape_start" bpmnElement="start"><dc:Bounds x="200" y="150" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="shape_end" bpmnElement="end"><dc:Bounds x="360" y="150" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="edge_1" bpmnElement="flow1"><di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="236" y="168" /><di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="360" y="168" /></bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await loadProcessDefs()
  await nextTick()
  initModeler()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (modeler.value) { modeler.value.destroy(); modeler.value = null }
})

const initModeler = () => {
  if (modeler.value) { modeler.value.destroy(); modeler.value = null }
  try {
    modeler.value = new BpmnModeler({
      container: '#canvas-container',
      propertiesPanel: { parent: '#properties-panel' },
      additionalModules: [BpmnPropertiesPanelModule, BpmnPropertiesProviderModule],
      moddleExtensions: { camunda: camundaModdle }
    })
    modeler.value.on('selection.changed', updateUndoRedo)
    modeler.value.on('commandStack.changed', updateUndoRedo)
    loadDiagram(defaultXml())
  } catch (err) {
    console.error('Modeler init error:', err)
    message.error('初始化建模器失败')
  }
}

const updateUndoRedo = () => {
  if (!modeler.value) return
  const stack = modeler.value.get('commandStack')
  canUndo.value = stack.canUndo()
  canRedo.value = stack.canRedo()
}

const undo = () => modeler.value?.get('commandStack').undo()
const redo = () => modeler.value?.get('commandStack').redo()
const zoomIn = () => { const c = modeler.value?.get('canvas'); const z = c.zoom(); c.zoom(z + 0.2) }
const zoomOut = () => { const c = modeler.value?.get('canvas'); const z = c.zoom(); c.zoom(z - 0.2) }
const zoomReset = () => modeler.value?.get('canvas').zoom('fit-viewport')

const loadDiagram = async (xml: string) => {
  if (!modeler.value) return
  try {
    await modeler.value.importXML(xml)
    modeler.value.get('canvas').zoom('fit-viewport')
    updateUndoRedo()
  } catch (err) { console.error('BPMN import error:', err); message.error('加载流程图失败') }
}

const loadProcessDefs = async () => {
  loading.value = true
  try {
    const res = await request.get('/v1/flowable/process-definition/list')
    if (res.code === 200) {
      const list = res.data
      processDefs.value = Array.isArray(list) ? list : (list.records || [])
    }
  } catch (err) { console.error(err) }
  finally { loading.value = false }
}

const handleCreateNew = () => {
  currentDefId.value = ''
  processName.value = '新流程'
  processKey.value = 'process_' + Date.now()
  initModeler()
}

const handleSelect = async (item: any) => {
  currentDefId.value = item.id
  processName.value = item.name || item.key
  processKey.value = item.key
  initModeler()
  await nextTick()
  try {
    const res = await request.get('/v1/flowable/process-definition/' + item.id + '/xml')
    if (res.code === 200 && res.data) { loadDiagram(res.data); return }
  } catch { /* fallback */ }
  loadDiagram(defaultXml())
}

const handleSave = async () => {
  try {
    const { xml } = await modeler.value.saveXML({ format: true })
    localStorage.setItem('bpmn_xml_' + processKey.value, xml)
    message.success('已保存到本地')
  } catch { message.error('保存失败') }
}

const handleDeploy = async () => {
  try {
    const { xml } = await modeler.value.saveXML({ format: true })
    const blob = new Blob([xml], { type: 'application/xml' })
    const file = new File([blob], `${processKey.value}.bpmn20.xml`, { type: 'application/xml' })
    const fd = new FormData()
    fd.append('file', file)
    const res = await deployProcessDefinition(fd)
    if (res.code === 200) { message.success('部署成功！ID: ' + res.data); await loadProcessDefs() }
    else message.error('部署失败: ' + res.message)
  } catch (err: any) { message.error('部署失败: ' + (err.message || err)) }
}

const handleExport = async () => {
  try {
    const { xml } = await modeler.value.saveXML({ format: true })
    const blob = new Blob([xml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = (processName.value || 'process') + '.bpmn20.xml'
    a.click(); URL.revokeObjectURL(url)
  } catch { message.error('导出失败') }
}
</script>

<style scoped>
.h-full { height: 100%; }
.designer-wrapper {
  height: calc(100vh - 140px);
}

/* 移动端切换栏 */
.mobile-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 10;
}
.mobile-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  cursor: pointer;
  color: #8c8c8c;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}
.mobile-tab.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
  background: #e6f7ff;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.toolbar--mobile {
  flex-wrap: nowrap;
  overflow-x: auto;
  padding: 4px 0;
}
.toolbar--mobile :deep(.ant-btn) {
  font-size: 11px;
  padding: 0 6px;
  flex-shrink: 0;
}

/* 流程列表 */
.process-list { overflow-y: auto; height: calc(100% - 10px); }
.process-item {
  padding: 10px;
  margin-bottom: 6px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all .2s;
}
.process-item:hover { border-color: #1890ff; background: #e6f7ff; }
.process-item.active { border-color: #1890ff; background: #e6f7ff; }
.process-name { font-size: 13px; font-weight: 500; margin-bottom: 4px; }
.process-meta { display: flex; align-items: center; gap: 4px; }

/* 属性面板 */
.properties-panel { overflow-y: auto; height: calc(100% - 20px); }
#canvas-container { background: #f9f9f9; }

/* 移动端适配 */
@media (max-width: 767px) {
  .designer-wrapper {
    height: calc(100vh - 100px);
  }
  .process-list {
    max-height: calc(100vh - 260px);
  }
}
</style>
