import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { $t } from '#/locales';

/** 列表搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      label: '项目名称',
      fieldName: 'projectName',
      component: 'Input',
    },
    {
      label: '分包名称',
      fieldName: 'subcontractName',
      component: 'Input',
    },
    {
      label: '状态',
      fieldName: 'status',
      component: 'Select',
      componentProps: {
        options: [
          { label: '审批中', value: 'RUNNING' },
          { label: '已通过', value: 'APPROVED' },
          { label: '已取消', value: 'CANCEL' },
        ],
      },
    },
  ];
}

/** 列表列 */
export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { title: '变更ID', field: 'id', width: 90 },
    { title: '分包计划ID', field: 'planId', width: 110 },
    { title: '项目名称', field: 'projectName', minWidth: 160 },
    { title: '专业工程', field: 'specialtyEngineering', width: 140 },
    { title: '分包名称', field: 'subcontractName', width: 160 },
    { title: '分包模式', field: 'subcontractMode', width: 120 },
    { title: '队伍来源', field: 'teamSource', width: 120 },
    { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
    { title: '备注', field: 'remark', minWidth: 160 },
    {
      title: $t('common.operation'),
      field: 'action',
      fixed: 'right',
      width: 100,
      slots: { default: 'actions' },
    },
  ];
}
