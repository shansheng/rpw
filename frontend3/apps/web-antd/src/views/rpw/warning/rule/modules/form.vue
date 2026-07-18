<script lang="ts" setup>
import type { WarningRuleApi } from '#/api/rpw/warning/rule';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Button,
  Input,
  InputNumber,
  Select,
  Switch,
  Tag,
  message,
} from 'ant-design-vue';

import {
  createWarningRule,
  getWarningAttributes,
  getWarningRule,
  updateWarningRule,
  validateWarningRule,
} from '#/api/rpw/warning/rule';
import { WARNING_LEVEL_OPTIONS } from '../data';

defineOptions({ name: 'WarningRuleForm' });

const emit = defineEmits(['success']);
const formData = ref<WarningRuleApi.WarningRule>();

/** 表单数据 */
const form = ref<WarningRuleApi.WarningRule>({
  enabled: 1,
  priority: 100,
  warningLevel: 'YELLOW',
});

/** 属性元数据 */
const objectTypes = ref<WarningRuleApi.ObjectTypeOption[]>([]);
const systemAttributes = ref<WarningRuleApi.SystemAttribute[]>([]);
const attributes = ref<WarningRuleApi.AttributeMeta[]>([]);

/** 编辑器状态 */
const exprRef = ref<any>();
const validateMsg = ref<{ ok: boolean; text: string } | null>(null);
const validating = ref(false);

const getTitle = computed(() =>
  formData.value?.id ? '编辑预警规则' : '新建预警规则',
);

/** 函数 / 逻辑 / 运算符 插入按钮 */
const funcButtons = [
  { label: 'isnull()', insert: 'isnull()' },
  { label: 'today()', insert: 'today()' },
  { label: 'now()', insert: 'now()' },
  { label: 'ifelse()', insert: 'ifelse(,,)' },
];
const logicButtons = ['and', 'or', 'not'];
const opButtons = ['+', '-', '*', '/', '<', '>', '<=', '>=', '=', '!='];

async function loadMeta(objectType?: string) {
  const data = await getWarningAttributes(objectType);
  objectTypes.value = data.objectTypes ?? [];
  systemAttributes.value = data.systemAttributes ?? [];
  attributes.value = data.attributes ?? [];
}

onMounted(() => loadMeta());

watch(
  () => form.value.objectType,
  (v) => {
    if (v) loadMeta(v);
  },
);

/** 获取 textarea DOM 元素（兼容 antd-vue 不同版本） */
function getTextareaEl(): HTMLTextAreaElement | null {
  const inst = exprRef.value;
  if (!inst) return null;
  if (inst.textarea) return inst.textarea as HTMLTextAreaElement;
  const root = inst.$el;
  return (root?.querySelector('textarea') as HTMLTextAreaElement) ?? null;
}

/** 在光标处插入文本 */
function insertText(text: string) {
  const el = getTextareaEl();
  const cur = form.value.conditionExpr ?? '';
  if (!el) {
    form.value.conditionExpr = cur + text;
    return;
  }
  const start = el.selectionStart ?? cur.length;
  const end = el.selectionEnd ?? cur.length;
  form.value.conditionExpr = cur.slice(0, start) + text + cur.slice(end);
  nextTick(() => {
    el.focus();
    const pos = start + text.length;
    el.setSelectionRange(pos, pos);
  });
}

/** 校验表达式 */
async function handleValidate() {
  if (!form.value.objectType) {
    message.warning('请先选择预警对象');
    return;
  }
  if (!form.value.conditionExpr?.trim()) {
    message.warning('请输入规则条件表达式');
    return;
  }
  validating.value = true;
  try {
    const r = await validateWarningRule({
      objectType: form.value.objectType as string,
      conditionExpr: form.value.conditionExpr as string,
    });
    validateMsg.value = r.valid
      ? { ok: true, text: '表达式合法 ✓' }
      : { ok: false, text: `表达式错误：${r.message ?? ''}` };
  } finally {
    validating.value = false;
  }
}

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!form.value.name?.trim()) {
      message.error('请输入规则名称');
      return;
    }
    if (!form.value.objectType) {
      message.error('请选择预警对象');
      return;
    }
    if (!form.value.warningLevel) {
      message.error('请选择预警等级');
      return;
    }
    if (!form.value.conditionExpr?.trim()) {
      message.error('请输入规则条件表达式');
      return;
    }
    modalApi.lock();
    const data = { ...form.value } as WarningRuleApi.WarningRule;
    try {
      await (formData.value?.id
        ? updateWarningRule(data)
        : createWarningRule(data));
      await modalApi.close();
      emit('success');
      message.success('操作成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      validateMsg.value = null;
      return;
    }
    const data = modalApi.getData<WarningRuleApi.WarningRule>();
    if (!data || !data.id) {
      form.value = {
        enabled: 1,
        priority: 100,
        warningLevel: 'YELLOW',
        conditionExpr: '',
      };
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getWarningRule(data.id);
      form.value = {
        ...formData.value,
        conditionExpr: formData.value.conditionExpr ?? '',
      };
      if (formData.value.objectType) {
        await loadMeta(formData.value.objectType);
      }
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[760px]">
    <div class="flex flex-col gap-4 p-1">
      <!-- 基本信息 -->
      <div class="grid grid-cols-2 gap-4">
        <div class="flex flex-col gap-1">
          <label class="text-sm text-gray-600">规则名称</label>
          <Input
            v-model:value="form.name"
            placeholder="请输入规则名称"
            allow-clear
          />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-sm text-gray-600">预警对象</label>
          <Select
            v-model:value="form.objectType"
            :options="objectTypes"
            placeholder="请选择预警对象"
            allow-clear
          />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-sm text-gray-600">预警等级（THEN 部分）</label>
          <Select
            v-model:value="form.warningLevel"
            :options="WARNING_LEVEL_OPTIONS"
            placeholder="请选择预警等级"
          />
        </div>
        <div class="flex items-end gap-6">
          <div class="flex flex-col gap-1">
            <label class="text-sm text-gray-600">优先级</label>
            <InputNumber
              v-model:value="form.priority"
              :min="0"
              class="!w-32"
            />
          </div>
          <div class="flex items-center gap-2 pb-1">
            <Switch
              v-model:checked="form.enabled"
              :checked-value="1"
              :un-checked-value="0"
            />
            <span class="text-sm text-gray-600">启用</span>
          </div>
        </div>
      </div>

      <!-- 规则编辑器 -->
      <div class="rounded border border-gray-200 p-3">
        <div class="mb-2 flex items-center justify-between">
          <span class="text-sm font-medium"
            >规则条件（IF 部分）</span
          >
          <Button
            size="small"
            :loading="validating"
            @click="handleValidate"
          >
            校验语法
          </Button>
        </div>

        <!-- 对象属性 -->
        <div class="mb-2">
          <div class="mb-1 text-xs text-gray-400">对象属性（点击插入）</div>
          <div class="flex flex-wrap gap-1">
            <Tag
              v-for="attr in attributes"
              :key="attr.field"
              color="blue"
              class="cursor-pointer"
              @click="insertText(attr.label)"
            >
              {{ attr.label }}
            </Tag>
            <span
              v-if="!form.objectType"
              class="text-xs text-gray-400"
              >请选择预警对象后显示</span
            >
            <span
              v-else-if="attributes.length === 0"
              class="text-xs text-gray-400"
              >该对象暂无属性</span
            >
          </div>
        </div>

        <!-- 系统属性 -->
        <div class="mb-2">
          <div class="mb-1 text-xs text-gray-400">系统属性（点击插入）</div>
          <div class="flex flex-wrap gap-1">
            <Tag
              v-for="sa in systemAttributes"
              :key="sa.label"
              color="green"
              class="cursor-pointer"
              @click="insertText(sa.label)"
            >
              {{ sa.label }}
            </Tag>
          </div>
        </div>

        <!-- 函数 / 逻辑 / 运算符 -->
        <div class="mb-2 flex flex-wrap items-center gap-1">
          <Button
            v-for="fb in funcButtons"
            :key="fb.label"
            size="small"
            @click="insertText(fb.insert)"
            >{{ fb.label }}</Button
          >
          <span class="mx-1 text-gray-300">|</span>
          <Button
            v-for="lb in logicButtons"
            :key="lb"
            size="small"
            @click="insertText(lb + ' ')"
            >{{ lb }}</Button
          >
          <span class="mx-1 text-gray-300">|</span>
          <Button
            v-for="ob in opButtons"
            :key="ob"
            size="small"
            @click="insertText(' ' + ob + ' ')"
            >{{ ob }}</Button
          >
        </div>

        <Input.TextArea
          ref="exprRef"
          v-model:value="form.conditionExpr"
          :rows="4"
          placeholder="例如：isnull(实际招标日期) and 3 < 开始编制招标文件日期 - 当前日期 and 开始编制招标文件日期 - 当前日期 < 7"
        />

        <div
          v-if="validateMsg"
          class="mt-2 text-sm"
          :class="validateMsg.ok ? 'text-green-600' : 'text-red-600'"
        >
          {{ validateMsg.text }}
        </div>
        <div class="mt-2 text-xs text-gray-400">
          支持：isnull(x)、today()、now()、ifelse(条件,真值,假值)；and/or/not；+ - * /；日期相减得天数；比较支持链式（如 3 &lt; a &lt; 7）。
        </div>
      </div>

      <!-- 备注 -->
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-600">备注</label>
        <Input.TextArea
          v-model:value="form.remark"
          :rows="2"
          placeholder="请输入备注"
          allow-clear
        />
      </div>
    </div>
  </Modal>
</template>
