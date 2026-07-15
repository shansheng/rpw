import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 资源类型选项 */
export const RESOURCE_TYPE_OPTIONS = [
  { label: '物料', value: 'MATERIAL' },
  { label: '设备', value: 'EQUIPMENT' },
  { label: '五金', value: 'HARDWARE' },
  { label: '流转', value: 'CIRCULATION' },
  { label: '办公', value: 'OFFICE' },
  { label: '安全', value: 'SAFETY' },
  { label: '分包', value: 'SUBCONTRACT' },
  { label: '劳务', value: 'LABOR' },
];

/** 阈值类型选项 */
export const THRESHOLD_TYPE_OPTIONS = [
  { label: '比率', value: 'RATE' },
  { label: '日期', value: 'DATE' },
  { label: '数量', value: 'QUANTITY' },
];

/** 预警级别选项 */
export const WARNING_LEVEL_OPTIONS = [
  { label: '一般', value: 'GENERAL' },
  { label: '重要', value: 'IMPORTANT' },
  { label: '紧急', value: 'URGENT' },
];

/** 检查频率选项 */
export const CHECK_FREQUENCY_OPTIONS = [
  { label: '实时', value: 'REALTIME' },
  { label: '每日', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
];

/** 新增/修改预警规则的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'ruleName',
      label: '规则名称',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入规则名称',
      },
      rules: z.string().min(1, '规则名称不能为空').max(50),
    },
    {
      fieldName: 'resourceType',
      label: '资源类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: RESOURCE_TYPE_OPTIONS,
        placeholder: '请选择资源类型',
      },
      rules: z.string().min(1, '资源类型不能为空'),
    },
    {
      fieldName: 'projectId',
      label: '项目ID',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        placeholder: '请输入项目ID',
      },
    },
    {
      fieldName: 'thresholdType',
      label: '阈值类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: THRESHOLD_TYPE_OPTIONS,
        placeholder: '请选择阈值类型',
      },
      rules: z.string().min(1, '阈值类型不能为空'),
    },
    {
      fieldName: 'warningThreshold',
      label: '预警阈值',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        placeholder: '请输入预警阈值',
      },
      rules: z.number().min(0, '预警阈值不能为空'),
    },
    {
      fieldName: 'compareField',
      label: '比较字段',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入比较字段',
      },
    },
    {
      fieldName: 'actualField',
      label: '实际字段',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入实际字段',
      },
    },
    {
      fieldName: 'warningLevel',
      label: '预警级别',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: WARNING_LEVEL_OPTIONS,
        placeholder: '请选择预警级别',
      },
      rules: z.string().min(1, '预警级别不能为空'),
    },
    {
      fieldName: 'checkFrequency',
      label: '检查频率',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: CHECK_FREQUENCY_OPTIONS,
        placeholder: '请选择检查频率',
      },
      rules: z.string().min(1, '检查频率不能为空'),
    },
    {
      fieldName: 'enabled',
      label: '是否启用',
      component: 'Switch',
      componentProps: {
        checkedValue: 1,
        unCheckedValue: 0,
      },
      rules: z.number().default(1),
    },
    {
      fieldName: 'notifyWecom',
      label: '企业微信通知',
      component: 'Switch',
      componentProps: {
        checkedValue: 1,
        unCheckedValue: 0,
      },
      rules: z.number().default(0),
    },
    {
      fieldName: 'notifyUsers',
      label: '通知人员',
      component: 'Input',
      componentProps: {
        maxLength: 255,
        placeholder: '请输入通知人员',
      },
    },
    {
      fieldName: 'description',
      label: '规则描述',
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        placeholder: '请输入规则描述',
        rows: 3,
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'resourceType',
      label: '资源类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: RESOURCE_TYPE_OPTIONS,
        placeholder: '请选择资源类型',
      },
    },
    {
      fieldName: 'projectId',
      label: '项目ID',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        placeholder: '请输入项目ID',
      },
    },
    {
      fieldName: 'enabled',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
        placeholder: '请选择状态',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'ruleName',
      title: '规则名称',
      minWidth: 160,
    },
    {
      field: 'resourceType',
      title: '资源类型',
      minWidth: 120,
    },
    {
      field: 'projectId',
      title: '项目ID',
      width: 120,
      align: 'center',
    },
    {
      field: 'thresholdType',
      title: '阈值类型',
      width: 120,
    },
    {
      field: 'warningThreshold',
      title: '预警阈值',
      width: 120,
      align: 'center',
    },
    {
      field: 'warningLevel',
      title: '预警级别',
      width: 120,
    },
    {
      field: 'checkFrequency',
      title: '检查频率',
      width: 120,
    },
    {
      field: 'enabled',
      title: '状态',
      width: 100,
      align: 'center',
      formatter: ({ cellValue }) => (cellValue === 1 ? '启用' : '禁用'),
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
