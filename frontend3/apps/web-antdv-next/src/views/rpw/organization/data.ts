import type { VbenFormSchema } from '#/adapter/form';

import { z } from '#/adapter/form';

const NODE_TYPE_OPTIONS = [
  { label: '组织节点（局/公司/项目）', value: 1 },
  { label: '部门节点', value: 2 },
];

const STATUS_OPTIONS = [
  { label: '进行中', value: 1 },
  { label: '已完工', value: 2 },
  { label: '已暂停', value: 3 },
];

export interface OrgFormCtx {
  /** 节点类型：1组织 2部门 */
  nodeType: number;
  /** 推导后的组织级别：1局 2公司 3项目 */
  expectedLevel: number;
  /** 是否编辑态（编辑时节点类型不可改） */
  isEdit: boolean;
}

/** 新增/修改组织节点的表单 schema（按节点类型/级别动态展示项目字段） */
export function useFormSchema(ctx: OrgFormCtx): VbenFormSchema[] {
  const schema: VbenFormSchema[] = [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      component: 'Input',
      fieldName: 'parentId',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'orgName',
      label: '名称',
      component: 'Input',
      componentProps: { maxLength: 100, placeholder: '请输入名称' },
      rules: z.string().min(1, '名称不能为空').max(100),
    },
    {
      fieldName: 'nodeType',
      label: '节点类型',
      component: 'Select',
      componentProps: { options: NODE_TYPE_OPTIONS, disabled: ctx.isEdit },
      rules: z.number().min(1, '请选择节点类型'),
    },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      componentProps: { min: 0, class: 'w-full' },
    },
  ];

  // 项目节点（组织节点且级别为 3）额外展示项目属性
  if (ctx.nodeType === 1 && ctx.expectedLevel === 3) {
    schema.push(
      {
        fieldName: 'projectCode',
        label: '项目编码',
        component: 'Input',
        componentProps: { maxLength: 50, placeholder: '请输入项目编码' },
      },
      {
        fieldName: 'status',
        label: '状态',
        component: 'Select',
        componentProps: { options: STATUS_OPTIONS, allowClear: true },
      },
      {
        fieldName: 'planStartDate',
        label: '计划开始',
        component: 'DatePicker',
        componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      },
      {
        fieldName: 'planEndDate',
        label: '计划结束',
        component: 'DatePicker',
        componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      },
    );
  }

  return schema;
}
