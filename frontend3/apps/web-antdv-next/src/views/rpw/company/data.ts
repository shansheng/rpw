import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 新增/修改公司的表单 */
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
      fieldName: 'companyName',
      label: '公司名称',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入公司名称',
      },
      rules: z.string().min(1, '公司名称不能为空').max(50),
    },
    {
      fieldName: 'orgId',
      label: '所属组织',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
        placeholder: '请输入所属组织ID',
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'orgId',
      label: '所属组织',
      component: 'InputNumber',
      componentProps: {
        allowClear: true,
        placeholder: '请输入所属组织ID',
      },
    },
    {
      fieldName: 'companyName',
      label: '公司名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入公司名称',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'companyName',
      title: '公司名称',
      minWidth: 160,
    },
    {
      field: 'orgId',
      title: '所属组织',
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
      width: 160,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
