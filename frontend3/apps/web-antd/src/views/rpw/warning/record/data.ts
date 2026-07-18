import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

import {
  objectTypeLabel,
  warningLevelMeta,
} from '../rule/data';

/** 预警对象类型选项 */
export const OBJECT_TYPE_OPTIONS = [
  { label: '分包计划', value: 'SUBCONTRACT' },
  { label: '材料计划', value: 'MATERIAL' },
  { label: '设备计划', value: 'EQUIPMENT' },
  { label: '劳务计划', value: 'LABOR' },
  { label: '五金计划', value: 'HARDWARE' },
  { label: '周转材计划', value: 'CIRCULATION' },
  { label: '办公用品计划', value: 'OFFICE' },
  { label: '安全物资计划', value: 'SAFETY' },
];

/** 预警等级选项 */
export const WARNING_LEVEL_OPTIONS = [
  { label: '红色预警', value: 'RED' },
  { label: '橙色预警', value: 'ORANGE' },
  { label: '黄色预警', value: 'YELLOW' },
];

/** 状态选项 */
export const STATUS_OPTIONS = [
  { label: '待处理', value: 'PENDING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已忽略', value: 'IGNORED' },
];

/** 处理操作选项（仅允许置为已解决 / 已忽略） */
export const HANDLE_STATUS_OPTIONS = [
  { label: '已解决', value: 'RESOLVED' },
  { label: '已忽略', value: 'IGNORED' },
];

/** 状态 -> 显示名 + 颜色 */
export function statusMeta(value?: string): { label: string; color: string } {
  switch (value) {
    case 'PENDING':
      return { label: '待处理', color: 'red' };
    case 'RESOLVED':
      return { label: '已解决', color: 'green' };
    case 'IGNORED':
      return { label: '已忽略', color: 'default' };
    default:
      return { label: value ?? '', color: 'default' };
  }
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'objectType',
      label: '计划类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: OBJECT_TYPE_OPTIONS,
        placeholder: '请选择计划类型',
      },
    },
    {
      fieldName: 'warningLevel',
      label: '预警等级',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: WARNING_LEVEL_OPTIONS,
        placeholder: '请选择预警等级',
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
      fieldName: 'planName',
      label: '计划名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入计划名称',
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
      field: 'planName',
      title: '计划名称',
      minWidth: 180,
    },
    {
      field: 'objectType',
      title: '计划类型',
      width: 140,
      formatter: ({ cellValue }) => objectTypeLabel(cellValue as string),
    },
    {
      field: 'warningLevel',
      title: '预警等级',
      width: 120,
      slots: {
        default: ({ row }) => {
          const m = warningLevelMeta(row.warningLevel);
          return `<span style="color:${m.color === 'default' ? '#999' : m.color}">${m.label}</span>`;
        },
      },
    },
    {
      field: 'reason',
      title: '预警原因',
      minWidth: 300,
    },
    {
      field: 'status',
      title: '状态',
      width: 110,
      slots: {
        default: ({ row }) => {
          const m = statusMeta(row.status);
          return `<span style="color:${m.color === 'default' ? '#999' : m.color}">${m.label}</span>`;
        },
      },
    },
    {
      field: 'triggeredTime',
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
