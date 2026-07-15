import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 新增/修改分包资源计划的表单 */
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
    },
    {
      fieldName: 'subcontractName',
      label: '分包名称',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入分包名称',
      },
      rules: z.string().min(1, '分包名称不能为空').max(100),
    },
    {
      fieldName: 'workContent',
      label: '工作内容',
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        placeholder: '请输入工作内容',
        rows: 3,
      },
    },
    {
      fieldName: 'supplierCode',
      label: '供应商编码',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入供应商编码',
      },
    },
    {
      fieldName: 'supplierName',
      label: '供应商名称',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入供应商名称',
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
      component: 'Textarea',
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
      fieldName: 'subcontractName',
      label: '分包名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入分包名称',
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
      field: 'subcontractName',
      title: '分包名称',
      minWidth: 160,
    },
    {
      field: 'workContent',
      title: '工作内容',
      minWidth: 200,
    },
    {
      field: 'supplierCode',
      title: '供应商编码',
      minWidth: 140,
    },
    {
      field: 'supplierName',
      title: '供应商名称',
      minWidth: 160,
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
      field: 'actualStartDate',
      title: '实际开始',
      width: 120,
    },
    {
      field: 'actualEndDate',
      title: '实际结束',
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
