<template>
  <div class="file-grid" :class="{ 'is-selectable': selectable }">
    <div
      v-for="file in files"
      :key="file.fileId"
      class="file-card"
      :class="{ 'is-selected': selectedIds.includes(file.fileId) }"
      @click="handleClick(file)"
    >
      <div v-if="selectable" class="file-card-checkbox">
        <el-checkbox
          :model-value="selectedIds.includes(file.fileId)"
          @change="() => toggleSelect(file)"
          @click.stop
        />
      </div>
      <div class="file-card-icon">
        <el-icon :size="40"><Document /></el-icon>
      </div>
      <div class="file-card-name" :title="file.fileName">{{ file.fileName }}</div>
      <div class="file-card-meta">
        <span>{{ file.size }}</span>
        <span v-if="file.contentLevel" class="file-card-level">
          <el-tag size="small" :type="getLevelType(file.contentLevel)">{{ file.contentLevel }}</el-tag>
        </span>
      </div>
      <div v-if="file.tags" class="file-card-tags">
        <el-tag v-for="tag in parseTags(file.tags)" :key="tag" size="small" type="info">{{ tag }}</el-tag>
      </div>
      <div class="file-card-actions" @click.stop>
        <slot name="actions" :file="file" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

interface FileItem {
  fileId: string
  fileName: string
  size: string
  downloadUrl: string
  uploadTime?: number
  uploader?: string
  contentLevel?: string
  library?: string
  tags?: any
  [key: string]: any
}

const props = withDefaults(defineProps<{
  files: FileItem[]
  selectable?: boolean
  selectedIds?: string[]
}>(), {
  selectable: false,
  selectedIds: () => []
})

const emit = defineEmits<{
  'click': [file: FileItem]
  'select': [file: FileItem]
}>()

const handleClick = (file: FileItem) => {
  if (props.selectable) {
    toggleSelect(file)
  } else {
    emit('click', file)
  }
}

const toggleSelect = (file: FileItem) => {
  emit('select', file)
}

const getLevelType = (level: string) => {
  switch (level) {
    case 'vvip': return 'danger'
    case 'svip': return 'warning'
    case 'vip': return 'success'
    default: return 'info'
  }
}

const parseTags = (tags: any) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.slice(0, 3).map((t: any) => t.name || t)
  try {
    return JSON.parse(tags).slice(0, 3).map((t: any) => t.name || t)
  } catch { return [] }
}
</script>

<style scoped>
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 16px;
  overflow-y: auto;
  max-height: calc(100vh - 280px);
}

.file-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.file-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--el-color-primary-light-5);
}

.file-card.is-selected {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.file-card-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
}

.file-card-icon {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.file-card-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
  margin-bottom: 4px;
}

.file-card-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.file-card-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
}

.file-card-actions {
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  justify-content: center;
  gap: 4px;
  flex-wrap: wrap;
}

.is-selectable .file-card {
  cursor: pointer;
}
</style>
