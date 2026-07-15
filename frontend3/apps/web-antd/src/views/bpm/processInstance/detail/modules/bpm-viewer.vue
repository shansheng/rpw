<script setup lang="ts">
import { ref, watch } from 'vue';

import { MyProcessViewer } from '#/views/bpm/components/bpmn-process-designer/package';

defineOptions({ name: 'ProcessInstanceBpmnViewer' });

const props = withDefaults(
  defineProps<{
    bpmnXml?: string;
    loading?: boolean; // 是否加载中
    modelView?: Record<string, any>;
  }>(),
  {
    loading: false,
    modelView: () => ({}),
    bpmnXml: '',
  },
);

// BPMN 流程图数据
const view = ref({
  bpmnXml: '',
});

/** 监控 modelView 更新 */
watch(
  () => props.modelView,
  async (newModelView) => {
    // 加载最新
    if (newModelView) {
      view.value = {
        ...newModelView,
        bpmnXml: newModelView.bpmnXml || '',
      };
    }
  },
  { immediate: true },
);

/** 监听 bpmnXml */
watch(
  () => props.bpmnXml,
  (value) => {
    view.value.bpmnXml = value || '';
  },
);
</script>

<template>
  <div
    v-loading="loading"
    class="h-full w-full overflow-auto rounded-lg border border-gray-200 bg-white p-4"
  >
    <!-- 流程图图例：常见待办流程图配色说明 -->
    <div class="bpm-legend mb-3 flex flex-wrap items-center gap-4 text-xs">
      <span class="bpm-legend-item">
        <i class="bpm-dot bpm-dot--success"></i>已完成
      </span>
      <span class="bpm-legend-item">
        <i class="bpm-dot bpm-dot--primary"></i>审批中
      </span>
      <span class="bpm-legend-item">
        <i class="bpm-dot bpm-dot--danger"></i>已驳回
      </span>
      <span class="bpm-legend-item">
        <i class="bpm-dot bpm-dot--cancel"></i>已取消
      </span>
    </div>
    <MyProcessViewer
      key="processViewer"
      :xml="view.bpmnXml || ''"
      :view="view"
      class="h-full min-h-[500px] w-full"
    />
  </div>
</template>

<style lang="scss" scoped>
.bpm-legend {
  color: #606266;

  .bpm-legend-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .bpm-dot {
    display: inline-block;
    width: 12px;
    height: 12px;
    border-radius: 3px;
    border: 2px solid transparent;

    &--success {
      background: rgba(78, 184, 25, 0.22);
      border-color: #4eb819;
    }

    &--primary {
      background: rgba(64, 158, 255, 0.25);
      border-color: #409eff;
    }

    &--danger {
      background: rgba(245, 108, 108, 0.2);
      border-color: #f56c6c;
    }

    &--cancel {
      background: rgba(144, 147, 153, 0.18);
      border-color: #909399;
    }
  }
}
</style>
