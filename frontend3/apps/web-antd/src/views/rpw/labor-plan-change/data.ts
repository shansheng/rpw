import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { LaborPlanChangeApi } from '#/api/rpw/labor-plan-change';

import { z } from '#/adapter/form';

/** 变更类型选项 */
export const CHANGE_TYPE_OPTIONS = [
  { label: '延期', value: 'DELAY' },
  { label: '数量变更', value: 'MODIFY_QUANTITY' },
  { label: '日期变更', value: 'MODIFY_DATE' },
];

/** 审批状态选项 */
export const APPROVAL_STATUS_OPTIONS = [
  { label: '待审批', value: 'PENDING' },
  { label: '通过', value: 'APPROVED' },
  { label: '驳回', value: 'REJECTED' },
];

/** 列表字段 */
export function useGridColumns(): VxeTableGridOptions<LaborPlanChangeApi.LaborPlanChange>['columns'] {
  return [
    { field: 'id', title: 'ID', width: 80 },
    {
      field: 'changeType',
      title: '变更类型',
      width: 120,
      formatter: ({ cellValue }) =>
        CHANGE_TYPE_OPTIONS.find((o) => o.value === cellValue)?.label ?? cellValue,
    },
    { field: 'changeReason', title: '变更原因', minWidth: 200 },
    {
      field: 'quantity',
      title: '人数(原→新)',
      width: 140,
      formatter: ({ row }) =>
        `${row.oldPlanQuantity ?? '-'} → ${row.newPlanQuantity ?? '-'}`,
    },
    {
      field: 'endDate',
      title: '结束日期(原→新)',
      minWidth: 220,
      formatter: ({ row }) =>
        `${row.oldPlanEndDate ?? '-'} → ${row.newPlanEndDate ?? '-'}`,
    },
    {
      field: 'approvalStatus',
      title: '审批状态',
      width: 120,
      formatter: ({ cellValue }) =>
        APPROVAL_STATUS_OPTIONS.find((o) => o.value === cellValue)?.label ?? cellValue,
    },
    { field: 'applicant', title: '申请人', width: 140 },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

/** 新增表单字段 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'laborPlanId',
      label: '劳动力计划ID',
      component: 'InputNumber',
      componentProps: {
        min: 1,
        placeholder: '请输入劳动力计划ID',
      },
      rules: z.number().min(1, '请输入劳动力计划ID'),
    },
    {
      fieldName: 'changeType',
      label: '变更类型',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: CHANGE_TYPE_OPTIONS,
        placeholder: '请选择变更类型',
      },
      rules: z.string().min(1, '请选择变更类型'),
    },
    {
      fieldName: 'changeReason',
      label: '变更原因',
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        rows: 3,
        placeholder: '请输入变更原因',
      },
    },
    {
      fieldName: 'newPlanStartDate',
      label: '新计划开始',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '请选择',
      },
    },
    {
      fieldName: 'newPlanEndDate',
      label: '新计划结束',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '请选择',
      },
    },
    {
      fieldName: 'newPlanQuantity',
      label: '新计划人数',
      component: 'InputNumber',
      componentProps: {
        min: 0,
        placeholder: '请输入',
      },
    },
  ];
}
