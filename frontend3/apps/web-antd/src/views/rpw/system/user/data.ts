import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { z } from '#/adapter/form';

/** 用户状态选项 */
export const USER_STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
];

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'username',
      label: '用户名',
      component: 'Input',
      componentProps: { placeholder: '请输入用户名', allowClear: true },
    },
    {
      fieldName: 'realName',
      label: '真实姓名',
      component: 'Input',
      componentProps: { placeholder: '请输入真实姓名', allowClear: true },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: USER_STATUS_OPTIONS,
        placeholder: '请选择状态',
      },
    },
  ];
}

/** 列表的字段
 * @param orgNameOf 组织ID -> 组织名称 的实时查询函数（用于展示所属组织）
 * @param roleNameOf 角色ID列表 -> 角色名称拼接 的实时查询函数（用于展示已绑定角色）
 */
export function useGridColumns(
  orgNameOf: (id?: number) => string,
  roleNameOf?: (ids?: number[]) => string,
): VxeTableGridOptions['columns'] {
  return [
    { title: '', width: 40, type: 'checkbox' },
    { field: 'id', title: 'ID', width: 80, align: 'center' },
    { field: 'username', title: '用户名', minWidth: 140 },
    { field: 'realName', title: '真实姓名', minWidth: 120 },
    {
      field: 'orgId',
      title: '所属组织',
      minWidth: 200,
      formatter: ({ row }: any) => orgNameOf(row.orgId as number | undefined),
    },
    { field: 'email', title: '邮箱', minWidth: 180 },
    { field: 'phone', title: '手机号', minWidth: 140 },
    {
      field: 'roleIds',
      title: '角色',
      minWidth: 200,
      formatter: ({ row }: any) =>
        roleNameOf ? roleNameOf(row.roleIds as number[] | undefined) : '-',
    },
    {
      field: 'status',
      title: '状态',
      width: 100,
      align: 'center',
      formatter: ({ cellValue }: any) => {
        return cellValue === 1 ? '启用' : cellValue === 0 ? '禁用' : '-';
      },
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

/** 新增/修改用户的表单
 * @param orgOptions 组织下拉选项（来自组织树扁平化）
 * @param roleOptions 角色下拉选项（来自角色精简列表）
 */
export function useFormSchema(
  orgOptions: { label: string; value: number }[],
  roleOptions: { label: string; value: number }[] = [],
): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'username',
      label: '用户名',
      component: 'Input',
      componentProps: { maxLength: 50, placeholder: '请输入登录用户名' },
      rules: z.string().min(1, '用户名不能为空').max(50),
    },
    {
      fieldName: 'realName',
      label: '真实姓名',
      component: 'Input',
      componentProps: { maxLength: 50, placeholder: '请输入真实姓名' },
      rules: z.string().min(1, '真实姓名不能为空').max(50),
    },
    {
      fieldName: 'orgId',
      label: '所属组织',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: orgOptions,
        placeholder: '请选择所属组织（可不选）',
        showSearch: true,
        optionFilterProp: 'label',
      },
    },
    {
      fieldName: 'email',
      label: '邮箱',
      component: 'Input',
      componentProps: { maxLength: 100, placeholder: '请输入邮箱' },
    },
    {
      fieldName: 'phone',
      label: '手机号',
      component: 'Input',
      componentProps: { maxLength: 20, placeholder: '请输入手机号' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: USER_STATUS_OPTIONS },
      rules: z.number().min(0, '请选择状态'),
      defaultValue: 1,
    },
    {
      fieldName: 'roleIds',
      label: '角色',
      component: 'Select',
      componentProps: {
        mode: 'multiple',
        allowClear: true,
        options: roleOptions,
        placeholder: '请选择角色（可多选）',
        showSearch: true,
        optionFilterProp: 'label',
      },
    },
    {
      fieldName: 'password',
      label: '登录密码',
      component: 'InputPassword',
      componentProps: {
        maxLength: 50,
        placeholder: '创建时留空则默认 123456；修改时留空表示不修改',
      },
      help: '创建用户：留空使用默认口令 123456。修改用户：留空则不修改密码。',
    },
  ];
}
