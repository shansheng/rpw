<script lang="ts" setup>
import type { BpmProcessInstanceApi } from '#/api/bpm/processInstance';
import type { ResourcePlanSubcontract } from '#/api/rpw/resource-plan';
import type {
  SubcontractChangeCreate,
  SubcontractChangeDetail,
} from '#/api/rpw/resource-plan/subcontract-change';

import { computed, onMounted, ref, watch } from 'vue';

import { confirm, Page } from '@vben/common-ui';
import { useTabs } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  InputNumber,
  message,
  Row,
  Space,
  Table,
} from 'antdv-next';
import dayjs from 'dayjs';

import { getProcessDefinition } from '#/api/bpm/definition';
import { getApprovalDetail as getApprovalDetailApi } from '#/api/bpm/processInstance';
import { subcontractApi } from '#/api/rpw/resource-plan';
import {
  dateTypeLabel,
  DATE_TYPE_OPTIONS,
  subcontractChangeApi,
} from '#/api/rpw/resource-plan/subcontract-change';
import { router } from '#/router';
import ProcessInstanceTimeline from '#/views/bpm/processInstance/detail/modules/time-line.vue';

import OrganizationSelectModal from '../subcontract/modules/organization-select-modal.vue';
import DateTypeModal from './modules/date-type-modal.vue';
import PlanSelectModal from './modules/plan-select-modal.vue';

defineOptions({ name: 'RpwResourcePlanSubcontractChangeCreate' });

const { closeCurrentTab } = useTabs();

const formLoading = ref(false);
const processTimeLineLoading = ref(false);

// ===== 主表 =====
const master = ref({
  planId: undefined as number | undefined,
  projectId: undefined as number | undefined,
  projectName: '' as string,
  specialtyEngineering: '' as string,
  subcontractName: '' as string,
  subcontractMode: '' as string,
  teamSource: '' as string,
  remark: '' as string,
});
const selectedPlan = ref<ResourcePlanSubcontract | null>(null);

// ===== 明细 =====
const details = ref<SubcontractChangeDetail[]>([]);

// ===== 弹窗可见性 =====
const orgModalVisible = ref(false);
const planModalVisible = ref(false);
const dateTypeModalVisible = ref(false);

// ===== 流程 =====
const processDefineKey = 'p_fbchange';
const startUserSelectTasks = ref<BpmProcessInstanceApi.ApprovalNodeInfo[]>([]);
const startUserSelectAssignees = ref<Record<string, number[]>>({});
const tempStartUserSelectAssignees = ref<Record<string, number[]>>({});
const activityNodes = ref<BpmProcessInstanceApi.ApprovalNodeInfo[]>([]);
const processDefinitionId = ref('');

// ===== 主表：选择项目 -> 选择分包计划 =====
function onProjectSelect(node: any) {
  master.value.projectId = node.id;
  master.value.projectName = node.orgName;
  // 切换项目后清空已选计划与明细
  master.value.planId = undefined;
  master.value.specialtyEngineering = '';
  master.value.subcontractName = '';
  master.value.subcontractMode = '';
  master.value.teamSource = '';
  selectedPlan.value = null;
  details.value = [];
  planModalVisible.value = true;
}

function onPlanSelect(plan: ResourcePlanSubcontract) {
  master.value.planId = plan.id;
  master.value.specialtyEngineering = plan.specialtyEngineering ?? '';
  master.value.subcontractName = plan.subcontractName ?? '';
  master.value.subcontractMode = plan.subcontractMode ?? '';
  master.value.teamSource = plan.teamSource ?? '';
  selectedPlan.value = plan;
  details.value = [];
  message.success(`已选择分包计划：${plan.subcontractName}`);
}

// ===== 明细：日期类型多选插入 =====
function openDateTypeModal() {
  if (!selectedPlan.value) {
    message.warning('请先选择分包计划');
    return;
  }
  dateTypeModalVisible.value = true;
}

/** 选中多个日期类型，按所选类型插入明细；明细中已存在的类型忽略 */
function onDateTypeSelect(payload: { dateTypes: number[] }) {
  const existing = new Set(details.value.map((d) => d.dateType));
  for (const t of payload.dateTypes) {
    if (existing.has(t)) continue; // 明细中已有的类型忽略
    const option = DATE_TYPE_OPTIONS.find((o) => o.value === t);
    const originalDate = option
      ? ((selectedPlan.value as any)?.[option.field] as string | undefined)
      : undefined;
    details.value.push({
      dateType: t,
      originalDate,
      adjustedDate: undefined,
    });
  }
}

function removeDetail(index: number) {
  details.value.splice(index, 1);
}

// ===== 流程相关 =====
async function getApprovalDetail() {
  processTimeLineLoading.value = true;
  try {
    const data = await getApprovalDetailApi({
      processDefinitionId: processDefinitionId.value,
      activityId: 'StartUserNode',
      processVariablesStr: JSON.stringify({}),
    });
    if (!data) {
      message.error('查询不到审批详情信息！');
      return;
    }
    activityNodes.value = data.activityNodes;
    startUserSelectTasks.value = data.activityNodes?.filter(
      (node: BpmProcessInstanceApi.ApprovalNodeInfo) => node.candidateStrategy === 35,
    );
    if (startUserSelectTasks.value?.length > 0) {
      for (const node of startUserSelectTasks.value) {
        const temp = tempStartUserSelectAssignees.value[node.id];
        startUserSelectAssignees.value[node.id] = temp?.length ? temp : [];
      }
    }
  } finally {
    processTimeLineLoading.value = false;
  }
}

function selectUserConfirm(id: string, userList: Array<{ id: number }>) {
  startUserSelectAssignees.value[id] = userList.map((item) => item.id);
}

// ===== 提交 =====
async function onSubmit() {
  const m = master.value;
  if (!m.projectName || m.planId == null) {
    message.warning('请先选择项目并选择分包计划');
    return;
  }
  const validDetails = details.value.filter((d) => d.dateType != null);
  if (validDetails.length === 0) {
    message.warning('请至少添加一条日期变更明细');
    return;
  }
  for (const d of validDetails) {
    if (!d.adjustedDate) {
      message.warning(`「${dateTypeLabel(d.dateType)}」的调整后日期不能为空`);
      return;
    }
  }
  if (startUserSelectTasks.value?.length > 0) {
    for (const task of startUserSelectTasks.value) {
      const assignees = startUserSelectAssignees.value[task.id];
      if (!Array.isArray(assignees) || assignees.length === 0) {
        message.warning(`请选择「${task.name}」的审批人`);
        return;
      }
    }
  }

  const payload: SubcontractChangeCreate = {
    planId: m.planId,
    projectId: m.projectId,
    projectName: m.projectName,
    specialtyEngineering: m.specialtyEngineering,
    subcontractName: m.subcontractName,
    subcontractMode: m.subcontractMode,
    teamSource: m.teamSource,
    remark: m.remark,
    details: validDetails.map((d) => ({
      dateType: d.dateType,
      originalDate: d.originalDate,
      adjustedDate: d.adjustedDate,
    })),
    startUserSelectAssignees: startUserSelectAssignees.value,
  };

  try {
    formLoading.value = true;
    await subcontractChangeApi.create(payload);
    message.success('提交成功，变更申请已进入审批流程');
    await closeCurrentTab();
    await router.push({ path: '/rpw/resource-plan/subcontract-change' });
  } finally {
    formLoading.value = false;
  }
}

function onBack() {
  confirm({
    content: '确定要返回上一页吗？请先保存您填写的信息！',
    icon: 'warning',
    beforeClose({ isConfirm }) {
      if (isConfirm) {
        closeCurrentTab();
      }
      return Promise.resolve(true);
    },
  });
}

// 明细表列
const detailColumns = [
  { title: '序号', key: 'seq', width: 60 },
  { title: '日期类型', key: 'dateType', width: 160 },
  { title: '原日期', key: 'originalDate', width: 140 },
  { title: '调整后日期', key: 'adjustedDate' },
  { title: '操作', key: 'action', width: 80 },
];

/** 调整后日期变更 */
function onAdjustedChange(index: number, v: string | undefined) {
  details.value[index] = { ...details.value[index], adjustedDate: v ?? undefined };
}

onMounted(async () => {
  const processDefinitionDetail: any = await getProcessDefinition(
    undefined,
    processDefineKey,
  );
  if (!processDefinitionDetail) {
    message.error('分包计划变更流程模型未配置，请检查！');
    return;
  }
  processDefinitionId.value = processDefinitionDetail.id;
  startUserSelectTasks.value = processDefinitionDetail.startUserSelectTasks;
  await getApprovalDetail();
});
</script>

<template>
  <Page>
    <Row :gutter="16">
      <Col :span="16">
        <Card title="主表信息" class="w-full" v-loading="formLoading">
          <template #extra>
            <Button type="default" @click="onBack">
              <IconifyIcon icon="lucide:arrow-left" />
              返回
            </Button>
          </template>

          <Row :gutter="16">
            <Col :span="12">
              <div class="mb-1 text-sm text-muted-foreground">分包计划ID</div>
              <InputNumber :value="master.planId" disabled class="w-full" />
            </Col>
            <Col :span="12">
              <div class="mb-1 text-sm text-muted-foreground">项目名称</div>
              <div class="flex w-full gap-2">
                <Input :value="master.projectName" placeholder="请选择项目" readonly class="flex-1" />
                <Button type="primary" @click="orgModalVisible = true">选择项目</Button>
              </div>
            </Col>
            <Col :span="12">
              <div class="mb-1 mt-3 text-sm text-muted-foreground">专业工程</div>
              <Input :value="master.specialtyEngineering" readonly />
            </Col>
            <Col :span="12">
              <div class="mb-1 mt-3 text-sm text-muted-foreground">分包名称</div>
              <Input :value="master.subcontractName" readonly />
            </Col>
            <Col :span="12">
              <div class="mb-1 mt-3 text-sm text-muted-foreground">分包模式</div>
              <Input :value="master.subcontractMode" readonly />
            </Col>
            <Col :span="12">
              <div class="mb-1 mt-3 text-sm text-muted-foreground">队伍来源</div>
              <Input :value="master.teamSource" readonly />
            </Col>
            <Col :span="24">
              <div class="mb-1 mt-3 text-sm text-muted-foreground">备注</div>
              <Input
                v-model:value="master.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入变更说明"
              />
            </Col>
          </Row>
        </Card>

        <Card title="变更明细" class="mt-4 w-full">
          <template #extra>
            <Button type="primary" @click="openDateTypeModal">
              <IconifyIcon icon="lucide:plus" />
              新增明细
            </Button>
          </template>
          <div class="max-h-[360px] overflow-y-auto">
            <Table
              :columns="detailColumns"
              :data-source="details"
              :pagination="false"
              :row-key="(record: any, index: number) => index"
              size="small"
              :locale="{ emptyText: '请点击「新增明细」选择日期类型' }"
            >
              <template #bodyCell="{ column, index }">
                <template v-if="column.key === 'seq'">{{ (index as number) + 1 }}</template>
                <template v-else-if="column.key === 'dateType'">
                  {{ dateTypeLabel(details[index as number]?.dateType) }}
                </template>
                <template v-else-if="column.key === 'originalDate'">
                  {{ details[index as number]?.originalDate || '—' }}
                </template>
                <template v-else-if="column.key === 'adjustedDate'">
                  <DatePicker
                    :value="details[index as number]?.adjustedDate"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                    placeholder="请选择"
                    @update:value="(v: string) => onAdjustedChange(index as number, v)"
                  />
                </template>
                <template v-else-if="column.key === 'action'">
                  <Button type="link" danger @click="removeDetail(index as number)">删除</Button>
                </template>
              </template>
            </Table>
          </div>
        </Card>
      </Col>

      <Col :span="8">
        <Card title="审批流程" class="w-full" v-loading="processTimeLineLoading">
          <ProcessInstanceTimeline
            :activity-nodes="activityNodes"
            :show-status-icon="false"
            @select-user-confirm="selectUserConfirm"
          />
          <template #actions>
            <Space warp :size="12" class="w-full px-6">
              <Button type="primary" :loading="formLoading" @click="onSubmit">
                提交
              </Button>
            </Space>
          </template>
        </Card>
      </Col>
    </Row>

    <OrganizationSelectModal v-model:visible="orgModalVisible" @select="onProjectSelect" />
    <PlanSelectModal
      v-model:visible="planModalVisible"
      :project-id="master.projectId"
      @select="onPlanSelect"
    />
    <DateTypeModal
      v-model:visible="dateTypeModalVisible"
      :plan="selectedPlan"
      :existing-types="details.map((d) => d.dateType as number)"
      @select="onDateTypeSelect"
    />
  </Page>
</template>
