import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 新增/修改安全资源计划的表单 */
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
      fieldName: 'wbsCode',
      label: 'WBS编码',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入WBS编码',
      },
    },
    {
      fieldName: 'safetyName',
      label: '安全用品名称',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入安全用品名称',
      },
      rules: z.string().min(1, '安全用品名称不能为空').max(100),
    },
    {
      fieldName: 'specification',
      label: '规格型号',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入规格型号',
      },
    },
    {
      fieldName: 'unit',
      label: '单位',
      component: 'Input',
      componentProps: {
        maxLength: 20,
        placeholder: '请输入单位',
      },
    },
    {
      fieldName: 'budgetQuantity',
      label: '预算数量',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
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
      fieldName: 'purchaseProgressCode',
      label: '采购进度编码',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入采购进度编码',
      },
    },
    {
      fieldName: 'planArrivalDate',
      label: '计划到场日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualArrivalDate',
      label: '实际到场日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
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
      fieldName: 'wbsCode',
      label: 'WBS编码',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入WBS编码',
      },
    },
    {
      fieldName: 'safetyName',
      label: '安全用品名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入安全用品名称',
      },
    },
    {
      fieldName: 'purchaseProgressCode',
      label: '采购进度',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入采购进度编码',
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
      field: 'wbsCode',
      title: 'WBS编码',
      minWidth: 140,
    },
    {
      field: 'safetyName',
      title: '安全用品名称',
      minWidth: 160,
    },
    {
      field: 'specification',
      title: '规格型号',
      minWidth: 140,
    },
    {
      field: 'unit',
      title: '单位',
      width: 80,
    },
    {
      field: 'budgetQuantity',
      title: '预算数量',
      width: 100,
    },
    {
      field: 'supplierCode',
      title: '供应商编码',
      minWidth: 140,
    },
    {
      field: 'purchaseProgressCode',
      title: '采购进度',
      minWidth: 120,
    },
    {
      field: 'planArrivalDate',
      title: '计划到场',
      width: 120,
    },
    {
      field: 'actualArrivalDate',
      title: '实际到场',
      width: 120,
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
