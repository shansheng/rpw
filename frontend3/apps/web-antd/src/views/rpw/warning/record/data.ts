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

/** 预警级别选项 */
export const WARNING_LEVEL_OPTIONS = [
  { label: '一般', value: 'GENERAL' },
  { label: '重要', value: 'IMPORTANT' },
  { label: '紧急', value: 'URGENT' },
];

/** 处理状态选项 */
export const STATUS_OPTIONS = [
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已忽略', value: 'IGNORED' },
];

/** 处理操作选项 */
export const HANDLE_STATUS_OPTIONS = [
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已忽略', value: 'IGNORED' },
];

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
      fieldName: 'warningLevel',
      label: '预警级别',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: WARNING_LEVEL_OPTIONS,
        placeholder: '请选择预警级别',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: STATUS_OPTIONS,
        placeholder: '请选择状态',
      },
    },
    {
      fieldName: 'startTime',
      label: '开始时间',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        placeholder: '请选择开始时间',
      },
    },
    {
      fieldName: 'endTime',
      label: '结束时间',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        placeholder: '请选择结束时间',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      title: '',
      width: 40,
      type: 'checkbox',
    },
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
      field: 'warningLevel',
      title: '预警级别',
      width: 120,
    },
    {
      field: 'status',
      title: '状态',
      width: 120,
      formatter: ({ cellValue }) => {
        const map: Record<string, string> = {
          PENDING: '待处理',
          PROCESSING: '处理中',
          RESOLVED: '已解决',
          IGNORED: '已忽略',
        };
        return map[cellValue as string] ?? cellValue;
      },
    },
    {
      field: 'triggerTime',
      title: '触发时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'handleTime',
      title: '处理时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 处理预警记录的表单 */
export function useHandleFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'status',
      label: '处理状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: HANDLE_STATUS_OPTIONS,
        placeholder: '请选择处理状态',
      },
      rules: z.string().min(1, '处理状态不能为空'),
    },
    {
      fieldName: 'handleRemark',
      label: '处理备注',
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        placeholder: '请输入处理备注',
        rows: 3,
      },
    },
  ];
}
