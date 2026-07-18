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
      label: '项目',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        disabled: true,
      },
      rules: z.number().min(1, '请选择项目'),
    },
    {
      fieldName: 'projectName',
      label: '项目名称',
      component: 'Input',
      componentProps: {
        maxLength: 200,
        placeholder: '点击选择项目',
        readonly: true,
        class: 'cursor-pointer',
      },
    },
    {
      fieldName: 'specialtyEngineering',
      label: '专业工程',
      component: 'Input',
      componentProps: {
        maxLength: 200,
        placeholder: '请输入专业工程',
      },
      rules: z.string().min(1, '专业工程不能为空').max(200),
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
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入分包模式',
      },
    },
    {
      fieldName: 'teamSource',
      label: '分包队伍来源',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入分包队伍来源',
      },
    },
    {
      fieldName: 'latestEntryDate',
      label: '最晚进场日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '与总进度计划相符',
      },
    },
    {
      fieldName: 'actualEntryDate',
      label: '实际进场日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
        placeholder: '按招标进度推算',
      },
    },
    {
      fieldName: 'startPrepareBidDate',
      label: '开始编制招标文件日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualBidDate',
      label: '实际招标日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'plannedOnlineBidDate',
      label: '挂网招标日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualOnlineBidDate',
      label: '实际挂网日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'plannedAwardDate',
      label: '定标日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'actualAwardDate',
      label: '实际定标日期',
      component: 'DatePicker',
      componentProps: {
        class: '!w-full',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'mobilizationPeriod',
      label: '动员期（天）',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        placeholder: '请输入动员期天数',
      },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        maxLength: 500,
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
      fieldName: 'projectName',
      label: '项目名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入项目名称',
      },
    },
    {
      fieldName: 'specialtyEngineering',
      label: '专业工程',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入专业工程',
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
      fieldName: 'subcontractMode',
      label: '分包模式',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入分包模式',
      },
    },
    {
      fieldName: 'teamSource',
      label: '分包队伍来源',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入分包队伍来源',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择状态',
        options: [
          { label: '草稿', value: 'DRAFT' },
          { label: '已提交', value: 'SUBMITTED' },
          { label: '进行中', value: 'IN_PROGRESS' },
          { label: '已完成', value: 'COMPLETED' },
          { label: '已终止', value: 'TERMINATED' },
        ],
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      type: 'seq',
      title: '序号',
      width: 60,
    },
    {
      field: 'projectName',
      title: '项目名称',
      minWidth: 160,
    },
    {
      field: 'specialtyEngineering',
      title: '专业工程',
      minWidth: 140,
    },
    {
      field: 'subcontractName',
      title: '分包名称',
      minWidth: 160,
    },
    {
      field: 'subcontractMode',
      title: '分包模式',
      minWidth: 120,
    },
    {
      field: 'teamSource',
      title: '分包队伍来源',
      minWidth: 120,
    },
    {
      field: 'latestEntryDate',
      title: '最晚进场日期（与总进度计划相符）',
      width: 160,
    },
    {
      field: 'actualEntryDate',
      title: '实际进场日期（按招标进度推算）',
      width: 160,
    },
    {
      field: 'startPrepareBidDate',
      title: '开始编制招标文件日期',
      width: 160,
    },
    {
      field: 'actualBidDate',
      title: '实际招标日期',
      width: 120,
    },
    {
      field: 'plannedOnlineBidDate',
      title: '挂网招标日期',
      width: 120,
    },
    {
      field: 'actualOnlineBidDate',
      title: '实际挂网日期',
      width: 120,
    },
    {
      field: 'plannedAwardDate',
      title: '定标日期',
      width: 120,
    },
    {
      field: 'actualAwardDate',
      title: '实际定标日期',
      width: 120,
    },
    {
      field: 'mobilizationPeriod',
      title: '动员期（天）',
      width: 100,
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 160,
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
