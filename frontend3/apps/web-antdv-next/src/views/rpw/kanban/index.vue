<script lang="ts" setup>
import type { KanbanApi } from '#/api/rpw/kanban';

import { onMounted, ref } from 'vue';

import { DocAlert, Page } from '@vben/common-ui';

import { Card, Col, Row, Select, Tag } from 'antdv-next';

import { getBoard } from '#/api/rpw/kanban';
import { $t } from '#/locales';

defineOptions({ name: 'KanbanBoard' });

const RESOURCE_TYPE_OPTIONS = [
  { label: '劳动力', value: 'LABOR' },
  { label: '材料', value: 'MATERIAL' },
  { label: '设备', value: 'EQUIPMENT' },
  { label: '五金', value: 'HARDWARE' },
  { label: '办公', value: 'OFFICE' },
  { label: '安全物资', value: 'SAFETY' },
  { label: '周转材', value: 'CIRCULATION' },
  { label: '分包', value: 'SUBCONTRACT' },
];

const resourceType = ref<string | undefined>(undefined);
const board = ref<KanbanApi.KanbanBoardVO>({});

async function loadBoard() {
  board.value = await getBoard({ resourceType: resourceType.value });
}

onMounted(loadBoard);
</script>

<template>
  <Page auto-content-height>
    <template #doc>
      <DocAlert title="【报表与看板】看板" url="https://doc.iocoder.cn/" />
    </template>

    <div class="mb-4 flex items-center gap-2">
      <span>资源类型：</span>
      <Select
        v-model:value="resourceType"
        :options="RESOURCE_TYPE_OPTIONS"
        allow-clear
        placeholder="全部"
        style="width: 200px"
        @change="loadBoard"
      />
      <span class="text-gray-400 text-xs">
        共 {{ board.totalCards || 0 }} 张卡片
      </span>
    </div>

    <Row :gutter="16">
      <Col
        v-for="col in board.columns || []"
        :key="col.statusKey"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <Card
          :title="`${col.statusName} (${(col.cards || []).length})`"
          size="small"
          class="mb-4"
        >
          <div
            v-for="card in col.cards || []"
            :key="card.id"
            class="mb-2 rounded border border-gray-200 p-2"
          >
            <div class="font-medium">{{ card.resourceName }}</div>
            <div class="text-xs text-gray-500">WBS: {{ card.wbsCode }}</div>
            <div class="mt-1 flex gap-1">
              <Tag color="blue">{{ card.resourceType }}</Tag>
              <Tag
                :color="
                  card.priority === 'HIGH'
                    ? 'red'
                    : card.priority === 'MEDIUM'
                      ? 'orange'
                      : 'green'
                "
              >
                {{ card.priority }}
              </Tag>
            </div>
            <div class="text-xs text-gray-400">负责人：{{ card.responsiblePerson }}</div>
            <div class="text-xs text-gray-400">
              {{ card.planStartDate }} ~ {{ card.planEndDate }}
            </div>
          </div>
        </Card>
      </Col>
    </Row>
  </Page>
</template>
