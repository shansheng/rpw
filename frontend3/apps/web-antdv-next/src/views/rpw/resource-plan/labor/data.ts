import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 新增/修改劳动力资源计划的表单 */
export function useFormSchema(_formApi?: VbenFormApi): VbenFormSchema[] {
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
      fieldName: 'projectId',
      label: '项目ID',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
      },
      rules: z.number().min(1, '项目ID不能为空'),
    },
    {
      fieldName: 'wbsName',
      label: 'WBS名称',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入WBS名称',
      },
      rules: z.string().min(1, 'WBS名称不能为空').max(100),
    },
    {
      fieldName: 'workTypeCode',
      label: '工种编码',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入工种编码',
      },
    },
    {
      fieldName: 'workTypeName',
      label: '工种名称',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入工种名称',
      },
    },
    {
      fieldName: 'laborCategoryCode',
      label: '劳务类别编码',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入劳务类别编码',
      },
    },
    {
      fieldName: 'laborCategoryName',
      label: '劳务类别名称',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入劳务类别名称',
      },
    },
    {
      fieldName: 'planQuantity',
      label: '计划数量',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
      },
    },
    {
      fieldName: 'actualQuantity',
      label: '实际数量',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
      },
    },
    {
      fieldName: 'planStartDate',
      label: '计划开始日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'planEndDate',
      label: '计划结束日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualStartDate',
      label: '实际开始日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualEndDate',
      label: '实际结束日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'approvalStatus',
      label: '审批状态',
      component: 'Input',
      componentProps: {
        maxLength: 20,
        placeholder: '请输入审批状态',
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'TextArea',
      componentProps: {
        maxLength: 255,
        placeholder: '请输入备注',
        rows: 3,
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'projectId',
      label: '项目ID',
      component: 'InputNumber',
      componentProps: {
        allowClear: true,
        placeholder: '请输入项目ID',
      },
    },
    {
      fieldName: 'wbsName',
      label: 'WBS名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入WBS名称',
      },
    },
    {
      fieldName: 'workTypeName',
      label: '工种名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入工种名称',
      },
    },
    {
      fieldName: 'approvalStatus',
      label: '审批状态',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入审批状态',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'projectId',
      title: '项目ID',
      minWidth: 100,
    },
    {
      field: 'wbsName',
      title: 'WBS名称',
      minWidth: 160,
    },
    {
      field: 'workTypeName',
      title: '工种名称',
      minWidth: 120,
    },
    {
      field: 'laborCategoryName',
      title: '劳务类别',
      minWidth: 120,
    },
    {
      field: 'planQuantity',
      title: '计划数量',
      width: 100,
    },
    {
      field: 'actualQuantity',
      title: '实际数量',
      width: 100,
    },
    {
      field: 'planStartDate',
      title: '计划开始',
      width: 120,
    },
    {
      field: 'planEndDate',
      title: '计划结束',
      width: 120,
    },
    {
      field: 'approvalStatus',
      title: '审批状态',
      minWidth: 120,
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 200,
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
