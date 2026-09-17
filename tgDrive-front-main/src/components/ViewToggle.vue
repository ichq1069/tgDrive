<template>
  <div class="view-toggle">
    <el-tooltip content="网格视图" placement="top">
      <el-button
        :type="modelValue === 'grid' ? 'primary' : 'default'"
        :icon="Grid"
        size="small"
        circle
        @click="$emit('update:modelValue', 'grid')"
      />
    </el-tooltip>
    <el-tooltip content="表格视图" placement="top">
      <el-button
        :type="modelValue === 'table' ? 'primary' : 'default'"
        :icon="List"
        size="small"
        circle
        @click="$emit('update:modelValue', 'table')"
      />
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { Grid, List } from '@element-plus/icons-vue'

defineProps<{
  modelValue: 'grid' | 'table'
}>()

defineEmits<{
  'update:modelValue': [value: 'grid' | 'table']
}>()
</script>

<style scoped>
.view-toggle {
  display: flex;
  gap: 4px;
  align-items: center;
}
</style>
