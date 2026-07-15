import type { VbenFormApi, VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 新增/修改组织的表单 */
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
      fieldName: 'orgName',
      label: '组织名称',
      component: 'Input',
      componentProps: {
        maxLength: 50,
        placeholder: '请输入组织名称',
      },
      rules: z.string().min(1, '组织名称不能为空').max(50),
    },
    {
      fieldName: 'orgLevel',
      label: '组织级别',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择组织级别',
        options: [
          { label: '1局', value: 1 },
          { label: '2公司', value: 2 },
          { label: '3项目', value: 3 },
        ],
      },
      rules: z.number().min(1, '组织级别不能为空'),
    },
    {
      fieldName: 'parentId',
      label: '上级组织',
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        min: 0,
      },
    },
    {
      fieldName: 'department',
      label: '部门',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入部门',
      },
    },
    {
      fieldName: 'section',
      label: '科室',
      component: 'Input',
      componentProps: {
        maxLength: 100,
        placeholder: '请输入科室',
      },
    },
  ];
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'orgLevel',
      label: '组织级别',
      component: 'Select',
      componentProps: {
        allowClear: true,
        placeholder: '请选择组织级别',
        options: [
          { label: '1局', value: 1 },
          { label: '2公司', value: 2 },
          { label: '3项目', value: 3 },
        ],
      },
    },
    {
      fieldName: 'orgName',
      label: '组织名称',
      component: 'Input',
      componentProps: {
        allowClear: true,
        placeholder: '请输入组织名称',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'orgName',
      title: '组织名称',
      minWidth: 160,
    },
    {
      field: 'orgLevel',
      title: '组织级别',
      width: 120,
      align: 'center',
      formatter: ({ cellValue }) => {
        if (cellValue === 1) return '1局';
        if (cellValue === 2) return '2公司';
        if (cellValue === 3) return '3项目';
        return '';
      },
    },
    {
      field: 'parentId',
      title: '上级组织',
      width: 120,
      align: 'center',
    },
    {
      field: 'department',
      title: '部门',
      minWidth: 160,
    },
    {
      field: 'section',
      title: '科室',
      minWidth: 160,
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
