import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

const SUBCONTRACT_MODE_OPTIONS = [
  { label: '劳务分包模式一', value: '劳务分包模式一' },
  { label: '劳务分包模式二', value: '劳务分包模式二' },
  { label: '专业分包', value: '专业分包' },
  { label: '其他', value: '其他' },
];

const TEAM_SOURCE_OPTIONS = [
  { label: '国内分包', value: '国内分包' },
  { label: '自有队伍', value: '自有队伍' },
  { label: '外部引进', value: '外部引进' },
  { label: '其他', value: '其他' },
];

/** 新增/修改表单 */
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
        placeholder: '请输入关联项目ID',
      },
      rules: z.number().min(1, '项目ID不能为空'),
    },
    {
      fieldName: 'professionalEngineering',
      label: '专业工程',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入专业工程',
      },
      rules: z.string().min(1, '专业工程不能为空').max(100),
    },
    {
      fieldName: 'subcontractName',
      label: '分包名称',
      component: 'Input',
      componentProps: {
        maxLength: 200,
        placeholder: '请输入分包名称',
      },
      rules: z.string().min(1, '分包名称不能为空').max(200),
    },
    {
      fieldName: 'subcontractMode',
      label: '分包模式',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择分包模式',
        options: SUBCONTRACT_MODE_OPTIONS,
      },
    },
    {
      fieldName: 'teamSource',
      label: '分包队伍来源',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择分包队伍来源',
        options: TEAM_SOURCE_OPTIONS,
      },
    },
    {
      fieldName: 'latestEntryDate',
      label: '最晚进场日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择最晚进场日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualEntryDate',
      label: '实际进场日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择实际进场日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'tenderDocStartDate',
      label: '开始编制招标文件日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择开始编制招标文件日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'onlineTenderDate',
      label: '挂网招标日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择挂网招标日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'bidAwardDate',
      label: '定标日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        placeholder: '请选择定标日期',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'mobilizationPeriodDays',
      label: '动员期（天）',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        placeholder: '请输入动员期天数',
      },
      rules: z.number().min(0, '动员期不能为负数').optional(),
    },
    {
      fieldName: 'remarks',
      label: '备注',
      component: 'TextArea',
      componentProps: {
        maxLength: 500,
        placeholder: '请输入备注',
        rows: 3,
      },
    },
  ];
}

/** 搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'projectId',
      label: '项目ID',
      component: 'InputNumber',
      componentProps: {
        allowClear: true,
        class: '!w-full',
        placeholder: '请输入项目ID',
      },
    },
    {
      fieldName: 'professionalEngineering',
      label: '专业工程',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入专业工程',
      },
    },
  ];
}

/** 列表字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'id',
      title: '序号',
      width: 80,
      align: 'center',
      type: 'seq',
    },
    {
      field: 'projectId',
      title: '项目ID',
      width: 100,
      align: 'center',
    },
    {
      field: 'professionalEngineering',
      title: '专业工程',
      minWidth: 140,
    },
    {
      field: 'subcontractName',
      title: '分包名称',
      minWidth: 180,
    },
    {
      field: 'subcontractMode',
      title: '分包模式',
      width: 140,
      align: 'center',
    },
    {
      field: 'teamSource',
      title: '分包队伍来源',
      width: 120,
      align: 'center',
    },
    {
      field: 'latestEntryDate',
      title: '最晚进场日期',
      width: 140,
      align: 'center',
    },
    {
      field: 'actualEntryDate',
      title: '实际进场日期',
      width: 140,
      align: 'center',
    },
    {
      field: 'tenderDocStartDate',
      title: '开始编制招标文件日期',
      width: 170,
      align: 'center',
    },
    {
      field: 'onlineTenderDate',
      title: '挂网招标日期',
      width: 140,
      align: 'center',
    },
    {
      field: 'bidAwardDate',
      title: '定标日期',
      width: 120,
      align: 'center',
    },
    {
      field: 'mobilizationPeriodDays',
      title: '动员期（天）',
      width: 110,
      align: 'center',
    },
    {
      field: 'remarks',
      title: '备注',
      minWidth: 140,
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
