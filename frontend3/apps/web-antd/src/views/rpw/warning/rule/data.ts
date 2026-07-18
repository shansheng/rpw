import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

/** 预警对象类型选项 */
export const OBJECT_TYPE_OPTIONS = [
  { label: '分包计划', value: 'SUBCONTRACT' },
  { label: '材料计划', value: 'MATERIAL' },
  { label: '设备计划', value: 'EQUIPMENT' },
  { label: '劳务计划', value: 'LABOR' },
  { label: '五金计划', value: 'HARDWARE' },
  { label: '周转材计划', value: 'CIRCULATION' },
  { label: '办公用品计划', value: 'OFFICE' },
  { label: '安全物资计划', value: 'SAFETY' },
];

/** 预警等级选项 */
export const WARNING_LEVEL_OPTIONS = [
  { label: '红色预警', value: 'RED' },
  { label: '橙色预警', value: 'ORANGE' },
  { label: '黄色预警', value: 'YELLOW' },
];

/** 状态选项 */
export const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
];

/** 对象类型 -> 显示名 */
export function objectTypeLabel(value?: string): string {
  return OBJECT_TYPE_OPTIONS.find((o) => o.value === value)?.label ?? value ?? '';
}

/** 预警等级 -> 显示名 + 颜色 */
export function warningLevelMeta(value?: string): { label: string; color: string } {
  switch (value) {
    case 'RED':
      return { label: '红色预警', color: 'red' };
    case 'ORANGE':
      return { label: '橙色预警', color: 'orange' };
    case 'YELLOW':
      return { label: '黄色预警', color: 'gold' };
    default:
      return { label: value ?? '', color: 'default' };
  }
}

/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'objectType',
      label: '预警对象',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: OBJECT_TYPE_OPTIONS,
        placeholder: '请选择预警对象',
      },
    },
    {
      fieldName: 'warningLevel',
      label: '预警等级',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: WARNING_LEVEL_OPTIONS,
        placeholder: '请选择预警等级',
      },
    },
    {
      fieldName: 'enabled',
      label: '状态',
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: STATUS_OPTIONS,
        placeholder: '请选择状态',
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'name',
      title: '规则名称',
      minWidth: 180,
    },
    {
      field: 'objectType',
      title: '预警对象',
      width: 140,
      formatter: ({ cellValue }) => objectTypeLabel(cellValue as string),
    },
    {
      field: 'warningLevel',
      title: '预警等级',
      width: 120,
      slots: {
        default: ({ row }) => {
          const m = warningLevelMeta(row.warningLevel);
          return `<span style="color:${m.color === 'default' ? '#999' : m.color}">${m.label}</span>`;
        },
      },
    },
    {
      field: 'priority',
      title: '优先级',
      width: 90,
      align: 'center',
    },
    {
      field: 'conditionExpr',
      title: '规则条件',
      minWidth: 260,
    },
    {
      field: 'enabled',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: ({ cellValue }) => (cellValue === 1 ? '启用' : '禁用'),
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
