<script setup lang="ts">
interface Props {
  title: string
  value: number | string
  icon?: string
  color?: string
  suffix?: string
}

withDefaults(defineProps<Props>(), {
  color: '#1890ff',
  suffix: ''
})
</script>

<template>
  <a-card :bordered="false" class="stat-card">
    <div class="stat-card-inner">
      <div class="stat-card-body">
        <div class="stat-title">{{ title }}</div>
        <div class="stat-value" :style="{ color }">
          {{ value }}<span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
        </div>
      </div>
      <div v-if="icon" class="stat-icon" :style="{ color, opacity: 0.2 }">
        <component :is="icon" style="font-size: 48px" />
      </div>
    </div>
  </a-card>
</template>

<style scoped>
.stat-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  border-left: 4px solid v-bind('color');
  height: 100%;
}

.stat-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-card-body {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-suffix {
  font-size: 14px;
  font-weight: 400;
  margin-left: 4px;
}

.stat-icon {
  flex-shrink: 0;
  margin-left: 16px;
}
</style>
