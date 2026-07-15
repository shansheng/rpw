/**
 * 轻量 useVbenModal 实现（适配本脚手架 web-antdv-next）
 *
 * 本脚手架的 @vben/common-ui 未提供 useVbenModal，这里以 ant-design-vue 的
 * Modal 为基础实现一个兼容 vben 双模式用法的 hook：
 *  - OWNER 用法：useVbenModal({ connectedComponent }) 返回 [Modal, api]，
 *    渲染真实模态框并在内部渲染 connectedComponent。
 *  - CONNECTED 用法：在 connectedComponent 内部 useVbenModal({ onConfirm, onOpenChange })，
 *    通过 provide/inject 复用父级 api，并注册回调。
 */
import {
  defineComponent,
  h,
  inject,
  provide,
  reactive,
  watch,
  type Component,
  type PropType,
} from 'vue';

import { Modal as AModal } from 'antdv-next';

const MODAL_API_KEY = Symbol('vben-modal-api');

export interface ModalState {
  isOpen: boolean;
  loading: boolean;
  title: string;
  width: string | number;
  data: any;
  connectedComponent: Component | undefined;
  destroyOnClose: boolean;
}

export interface ModalApiOptions {
  connectedComponent?: Component;
  destroyOnClose?: boolean;
  onConfirm?: () => void | Promise<void>;
  onOpenChange?: (open: boolean) => void | Promise<void>;
  title?: string;
}

export class ModalApi {
  state: ModalState;
  private handlers: {
    onConfirm?: () => void | Promise<void>;
    onOpenChange?: (open: boolean) => void | Promise<void>;
  } = {};

  constructor(initial: Partial<ModalState> = {}) {
    this.state = reactive({
      isOpen: false,
      loading: false,
      title: '',
      width: '40%',
      data: undefined,
      connectedComponent: undefined,
      destroyOnClose: false,
      ...initial,
    });
  }

  setData(data: any) {
    this.state.data = data;
    return this;
  }

  getData<T = any>(): T {
    return this.state.data as T;
  }

  setState(patch: Partial<ModalState>) {
    Object.assign(this.state, patch);
    return this;
  }

  getState() {
    return this.state;
  }

  async open() {
    // 仅切换打开状态；onOpenChange 由 OwnerModal 的 post-flush watcher 触发，
    // 确保 connectedComponent（Form）已挂载并注册好回调后再执行。
    this.state.isOpen = true;
  }

  async close() {
    this.state.isOpen = false;
  }

  async confirm() {
    await this.handlers.onConfirm?.();
  }

  lock() {
    this.state.loading = true;
  }

  unlock() {
    this.state.loading = false;
  }

  setHandlers(handlers: {
    onConfirm?: () => void | Promise<void>;
    onOpenChange?: (open: boolean) => void | Promise<void>;
  }) {
    this.handlers = { ...this.handlers, ...handlers };
  }
}

/** OWNER 模态框：渲染真实 a-modal，并在默认插槽中渲染 connectedComponent */
const OwnerModal = defineComponent({
  name: 'VbenModalOwner',
  props: {
    api: { type: Object as PropType<ModalApi>, required: true },
  },
  setup(props, { attrs }) {
    const api = props.api;
    provide(MODAL_API_KEY, api);
    // 以 post-flush watcher 驱动 onOpenChange：确保 connectedComponent（Form）已挂载、
    // 其内部 useVbenModal 注册的回调已就绪后，再执行 onOpenChange（设置表单 schema/数据）。
    watch(
      () => api.state.isOpen,
      async (open) => {
        await api.handlers.onOpenChange?.(open);
      },
      { flush: 'post' },
    );
    return () => {
      const Comp = api.state.connectedComponent as Component | undefined;
      const { onSuccess, ...restAttrs } = attrs as Record<string, any>;
      return h(
        AModal,
        {
          open: api.state.isOpen,
          title: api.state.title,
          width: api.state.width,
          confirmLoading: api.state.loading,
          maskClosable: false,
          destroyOnClose: api.state.destroyOnClose,
          onOk: () => api.confirm(),
          onCancel: () => api.close(),
          ...restAttrs,
        },
        {
          default: () =>
            Comp ? h(Comp, { ...restAttrs, onSuccess }) : null,
        },
      );
    };
  },
});

/** CONNECTED 桥接组件：渲染插槽内容，并把视觉属性（title/width）同步到真实模态框 */
const BridgeModal = defineComponent({
  name: 'VbenModalBridge',
  setup(_, { slots, attrs }) {
    const api = inject<ModalApi | null>(MODAL_API_KEY, null);
    if (api) {
      watch(
        () => (attrs as Record<string, any>).title,
        (v) => {
          if (v !== undefined) api.setState({ title: v as string });
        },
        { immediate: true },
      );
      watch(
        () => (attrs as Record<string, any>).width,
        (v) => {
          if (v !== undefined) api.setState({ width: v as string | number });
        },
        { immediate: true },
      );
    }
    return () => (slots.default ? slots.default() : null);
  },
});

let uid = 0;

export function useVbenModal(options: ModalApiOptions = {}) {
  const injected = inject<ModalApi | null>(MODAL_API_KEY, null);

  // CONNECTED 用法：在 connectedComponent 内部复用父级 api
  if (!options.connectedComponent && injected) {
    injected.setHandlers({
      onConfirm: options.onConfirm,
      onOpenChange: options.onOpenChange,
    });
    return [BridgeModal, injected] as const;
  }

  // OWNER 用法
  const api = new ModalApi({
    connectedComponent: options.connectedComponent,
    destroyOnClose: options.destroyOnClose ?? false,
    title: options.title ?? '',
  });
  if (options.onConfirm || options.onOpenChange) {
    api.setHandlers({
      onConfirm: options.onConfirm,
      onOpenChange: options.onOpenChange,
    });
  }

  const ModalComp = defineComponent({
    name: `VbenModal-${++uid}`,
    setup(_, { attrs, slots }) {
      provide(MODAL_API_KEY, api);
      return () => h(OwnerModal, { api, ...attrs }, slots);
    },
  });

  return [ModalComp, api] as const;
}

export default useVbenModal;
