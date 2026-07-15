<script lang="ts" setup>
import type { BpmOALeaveApi } from '#/api/bpm/oa/leave';

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { ContentWrap } from '@vben/common-ui';

import { Spin } from 'ant-design-vue';

import { getLeave, getLeaveByProcessInstanceId } from '#/api/bpm/oa/leave';
import { useDescription } from '#/components/description';

import { useDetailFormSchema } from './data';

const props = defineProps<{
  id?: string;
  processInstanceId?: string;
}>();

const { query } = useRoute();

const loading = ref(false);
const formData = ref<BpmOALeaveApi.Leave>();
const queryId = computed(() => query.id as string);

const [Descriptions] = useDescription({
  bordered: true,
  column: 1,
  class: 'mx-4',
  schema: useDetailFormSchema(),
});

/** 获取详情数据：优先用请假ID，其次按流程实例ID回查业务表单 */
async function getDetailData() {
  try {
    loading.value = true;
    formData.value = null;
    const leaveId = props.id || queryId.value;
    if (leaveId) {
      formData.value = await getLeave(Number(leaveId));
    } else if (props.processInstanceId) {
      formData.value = await getLeaveByProcessInstanceId(props.processInstanceId);
    }
  } finally {
    loading.value = false;
  }
}

/** 初始化 */
onMounted(() => {
  getDetailData();
});
</script>

<template>
  <ContentWrap class="m-2">
    <Spin :spinning="loading" tip="加载中...">
      <Descriptions :data="formData" />
    </Spin>
  </ContentWrap>
</template>
