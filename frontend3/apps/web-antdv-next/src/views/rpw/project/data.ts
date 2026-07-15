import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

const STATUS_OPTIONS = [
  { label: '1进行中', value: 1 },
  { label: '2已完工', value: 2 },
  { label: '3已暂停', value: 3 },
];

/** 新增/修改项目的表单 */
export function useFormSchema(formApi?: VbenFormApi): VbenFormSchema[] {
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
      fieldName: 'projectName',
      label: '项目名称',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入项目名称',
      },
      rules: z.string().min(1, '项目名称不能为空').max(50),
    },
    {
      fieldName: 'companyId',
      label: '所属公司',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        placeholder: '请输入所属公司ID',
      },
      rules: z.number().min(1, '所属公司不能为空'),
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择状态',
        options: STATUS_OPTIONS,
      },
      rules: z.number().min(1, '状态不能为空'),
    },
    {
      fieldName: 'planStartDate',
      label: '计划开始日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择计划开始日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'planEndDate',
      label: '计划结束日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择计划结束日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'companyId',
      label: '所属公司',
      component: 'InputNumber',
      componentProps: {
        allowClear: true,
        placeholder: '请输入所属公司ID',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择状态',
        options: STATUS_OPTIONS,
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'projectName',
      title: '项目名称',
      minWidth: 160,
    },
    {
      field: 'companyId',
      title: '所属公司',
      width: 140,
      align: 'center',
    },
    {
      field: 'status',
      title: '状态',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => {
        if (cellValue === 1) return '1进行中';
        if (cellValue === 2) return '2已完工';
        if (cellValue === 3) return '3已暂停';
        return '';
      },
    },
    {
      field: 'planStartDate',
      title: '计划开始日期',
      width: 140,
      align: 'center',
    },
    {
      field: 'planEndDate',
      title: '计划结束日期',
      width: 140,
      align: 'center',
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 240,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
