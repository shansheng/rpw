<template>
  <div class="designer-container">
    <el-row :gutter="12">
      <!-- 左侧：流程列表 -->
      <el-col :span="4">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>流程定义</span>
              <el-button type="primary" size="small" @click="handleCreateNew">新建</el-button>
            </div>
          </template>
          <div class="process-list">
            <div v-if="loading" style="text-align:center;padding:20px;">
              <el-icon class="is-loading"><Loading /></el-icon>
              <p>加载中...</p>
            </div>
            <div v-else-if="processDefs.length === 0" style="text-align:center;padding:20px;color:#999;font-size:13px;">
              暂无流程，点击"新建"创建
            </div>
            <div v-for="item in processDefs" :key="item.id"
                 class="process-item"
                 :class="{ active: currentDefId === item.id }"
                 @click="handleSelect(item)">
              <div class="process-name">{{ item.name || item.key }}</div>
              <div class="process-meta">
                <el-tag size="small" type="info">v{{ item.version }}</el-tag>
                <span class="process-key">{{ item.key }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间：画布 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="toolbar">
              <el-space :size="8">
                <el-button size="small" @click="undo" :disabled="!canUndo">撤销</el-button>
                <el-button size="small" @click="redo" :disabled="!canRedo">重做</el-button>
                <el-divider direction="vertical" />
                <el-button size="small" @click="zoomIn">放大</el-button>
                <el-button size="small" @click="zoomOut">缩小</el-button>
                <el-button size="small" @click="zoomReset">适应</el-button>
                <el-divider direction="vertical" />
                <el-button size="small" type="success" @click="handleSave">保存</el-button>
                <el-button size="small" type="primary" @click="handleDeploy">部署</el-button>
                <el-button size="small" @click="handleExport">导出</el-button>
                <el-upload
                  style="display:inline-block;"
                  accept=".bpmn,.xml"
                  :auto-upload="false"
                  :on-change="handleImport"
                  :show-file-list="false"
                >
                  <el-button size="small">导入</el-button>
                </el-upload>
              </el-space>
              <el-space :size="8">
                <el-tag size="small" type="info">{{ processName || '未命名' }}</el-tag>
              </el-space>
            </div>
          </template>
          <div class="canvas-wrapper">
            <div id="canvas" class="canvas"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：属性面板 -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>属性面板</span>
            </div>
          </template>
          <div class="properties-panel" id="properties-panel"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule
} from 'bpmn-js-properties-panel'
import camundaModdle from 'camunda-bpmn-moddle/resources/camunda.json'

// CSS
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

import { deployProcessDefinition, saveProcessDefinition, listProcessDefinitions, getProcessDefinitionXml } from '@/api/flowable'

const modeler = ref<any>(null)
const processName = ref('新流程')
const processKey = ref('process_' + Date.now())
const processDefs = ref<any[]>([])
const currentDefId = ref<string>('')
const loading = ref(false)
const canUndo = ref(false)
const canRedo = ref(false)

// 创建默认空流程图 XML
const createDefaultXml = (key: string, name: string) => `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
  xmlns:flowable="http://flowable.org/bpmn"
  targetNamespace="http://www.example.com" id="def_1">
  <bpmn:process id="${key}" name="${name}" isExecutable="true">
    <bpmn:startEvent id="startEvent_1" name="开始">
      <bpmn:outgoing>flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:sequenceFlow id="flow_1" sourceRef="startEvent_1" targetRef="endEvent_1" />
    <bpmn:endEvent id="endEvent_1" name="结束">
      <bpmn:incoming>flow_1</bpmn:incoming>
    </bpmn:endEvent>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="diagram_1">
    <bpmndi:BPMNPlane id="plane_1" bpmnElement="${key}">
      <bpmndi:BPMNShape id="start_shape_1" bpmnElement="startEvent_1">
        <dc:Bounds x="200" y="150" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="end_shape_1" bpmnElement="endEvent_1">
        <dc:Bounds x="350" y="150" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="edge_1" bpmnElement="flow_1">
        <di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="236" y="168" />
        <di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="350" y="168" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

onMounted(async () => {
  await loadProcessDefs()
  await nextTick()
  initModeler()
})

onUnmounted(() => {
  if (modeler.value) {
    modeler.value.destroy()
    modeler.value = null
  }
})

// 初始化建模器（含属性面板）
const initModeler = () => {
  if (modeler.value) {
    modeler.value.destroy()
    modeler.value = null
  }
  try {
    modeler.value = new BpmnModeler({
      container: '#canvas',
      propertiesPanel: {
        parent: '#properties-panel'
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule
      ],
      moddleExtensions: {
        camunda: camundaModdle
      }
    })

    // 监听选择变化，更新撤销状态
    modeler.value.on('selection.changed', () => {
      updateUndoRedo()
    })
    // 监听命令执行，更新撤销状态
    modeler.value.on('commandStack.changed', () => {
      updateUndoRedo()
    })

    loadDiagram(createDefaultXml(processKey.value, processName.value))
  } catch (err) {
    console.error('Modeler init error:', err)
    ElMessage.error('初始化建模器失败')
  }
}

// 更新撤销/重做状态
const updateUndoRedo = () => {
  if (!modeler.value) return
  const cmdStack = modeler.value.get('commandStack')
  canUndo.value = cmdStack.canUndo()
  canRedo.value = cmdStack.canRedo()
}

// 撤销
const undo = () => {
  if (modeler.value) modeler.value.get('commandStack').undo()
}

// 重做
const redo = () => {
  if (modeler.value) modeler.value.get('commandStack').redo()
}

// 放大
const zoomIn = () => {
  if (modeler.value) {
    const canvas = modeler.value.get('canvas')
    const cur = canvas.zoom()
    canvas.zoom(cur + 0.2)
  }
}

// 缩小
const zoomOut = () => {
  if (modeler.value) {
    const canvas = modeler.value.get('canvas')
    const cur = canvas.zoom()
    canvas.zoom(cur - 0.2)
  }
}

// 适应画布
const zoomReset = () => {
  if (modeler.value) {
    modeler.value.get('canvas').zoom('fit-viewport')
  }
}

// 加载流程图到画布
const loadDiagram = async (xml: string) => {
  if (!modeler.value) return
  try {
    await modeler.value.importXML(xml)
    const canvas = modeler.value.get('canvas')
    canvas.zoom('fit-viewport')
    updateUndoRedo()
  } catch (err) {
    console.error('BPMN import error:', err)
    ElMessage.error('加载流程图失败')
  }
}

// 加载流程定义列表
const loadProcessDefs = async () => {
  loading.value = true
  try {
    const res = await listProcessDefinitions()
    if (res.code === 200) {
      processDefs.value = res.data || []
    }
  } catch (err) {
    console.error('加载流程定义失败:', err)
  } finally {
    loading.value = false
  }
}

// 新建流程
const handleCreateNew = () => {
  currentDefId.value = ''
  processName.value = '新流程'
  processKey.value = 'process_' + Date.now()
  initModeler()
}

// 选择流程定义
const handleSelect = async (item: any) => {
  currentDefId.value = item.id
  processName.value = item.name || item.key
  processKey.value = item.key
  initModeler()
  // 延迟加载，等待建模器初始化完成
  await nextTick()
  try {
    const res = await getProcessDefinitionXml(item.id)
    if (res.code === 200 && res.data) {
      loadDiagram(res.data)
      return
    }
  } catch {
    const savedXml = localStorage.getItem('bpmn_xml_' + item.key)
    if (savedXml) {
      loadDiagram(savedXml)
    } else {
      loadDiagram(createDefaultXml(item.key, item.name))
    }
  }
}

// 保存
const handleSave = async () => {
  try {
    const { xml } = await modeler.value.saveXML({ format: true })
    saveProcessDefinition(processKey.value, xml)
    await loadProcessDefs()
    ElMessage.success('保存成功')
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

// 部署
const handleDeploy = async () => {
  try {
    const { xml } = await modeler.value.saveXML({ format: true })
    const blob = new Blob([xml], { type: 'application/xml' })
    const file = new File([blob], `${processKey.value}.bpmn20.xml`, { type: 'application/xml' })
    const formData = new FormData()
    formData.append('file', file)
    const res = await deployProcessDefinition(formData)
    if (res.code === 200) {
      ElMessage.success('部署成功！ID: ' + res.data)
      await loadProcessDefs()
    } else {
      ElMessage.error('部署失败: ' + res.message)
    }
  } catch (err) {
    ElMessage.error('部署失败: ' + (err instanceof Error ? err.message : err))
  }
}

// 导出
const handleExport = async () => {
  try {
    const { xml } = await modeler.value.saveXML({ format: true })
    const blob = new Blob([xml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (processName.value || 'process') + '.bpmn20.xml'
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    ElMessage.error('导出失败')
  }
}

// 导入
const handleImport = (file: any) => {
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const xml = e.target?.result as string
      await modeler.value.importXML(xml)
      modeler.value.get('canvas').zoom('fit-viewport')
      ElMessage.success('导入成功')
    } catch (err) {
      ElMessage.error('导入失败')
    }
  }
  reader.readAsText(file.raw)
}
</script>

<style scoped>
.designer-container {
  padding: 12px;
  height: calc(100vh - 80px);
}
.designer-container :deep(.el-row) {
  height: 100%;
}
.designer-container :deep(.el-col) {
  height: 100%;
}
.designer-container :deep(.el-card) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.designer-container :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 12px;
  display: flex;
  flex-direction: column;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.canvas-wrapper {
  flex: 1;
  position: relative;
  min-height: 300px;
}
.canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.properties-panel {
  flex: 1;
  overflow-y: auto;
  min-height: 100px;
}
.process-list {
  flex: 1;
  overflow-y: auto;
}
.process-item {
  padding: 10px;
  margin-bottom: 6px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.process-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}
.process-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.process-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}
.process-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}
.process-key {
  font-size: 11px;
  color: #999;
}
</style>
