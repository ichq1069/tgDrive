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
      <div class="file-card-preview">
        <img
          v-if="isImageFile(file.fileName)"
          :src="getFullUrl(file.downloadUrl)"
          :alt="file.fileName"
          class="file-thumbnail"
          loading="lazy"
          decoding="async"
          @error="handleImageError($event)"
          @click.stop="previewImage(file)"
        />
        <el-icon v-else :size="40"><Document /></el-icon>
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

  <!-- Image Preview Dialog -->
  <el-dialog v-model="previewVisible" title="图片预览" width="80%" top="5vh" destroy-on-close>
    <div class="preview-container">
      <img :src="previewUrl" :alt="previewName" class="preview-image" />
    </div>
    <template #footer>
      <span class="preview-name">{{ previewName }}</span>
      <el-button @click="previewVisible = false">关闭</el-button>
      <el-button type="primary" @click="downloadPreview">下载</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
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

const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')

const IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg', '.ico', '.tiff', '.tif']

const isImageFile = (fileName: string): boolean => {
  const ext = fileName.toLowerCase().split('.').pop()
  return IMAGE_EXTENSIONS.includes('.' + ext)
}

const getFullUrl = (url: string): string => {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  return window.location.origin + url
}

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

const previewImage = (file: FileItem) => {
  previewUrl.value = getFullUrl(file.downloadUrl)
  previewName.value = file.fileName
  previewVisible.value = true
}

const downloadPreview = () => {
  const link = document.createElement('a')
  link.href = previewUrl.value
  link.download = previewName.value
  link.click()
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
  const parent = img.parentElement
  if (parent) {
    const icon = document.createElement('div')
    icon.className = 'file-card-icon-fallback'
    icon.innerHTML = '<svg viewBox="0 0 24 24" width="40" height="40" fill="currentColor"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>'
    parent.appendChild(icon)
  }
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

.file-card-preview {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  border-radius: 4px;
  background-color: var(--el-fill-color-lighter);
}

.file-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}

.file-thumbnail:hover {
  transform: scale(1.05);
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

:deep(.file-card-icon-fallback) {
  color: var(--el-text-color-secondary);
}

/* Preview Dialog Styles */
.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  max-height: 70vh;
  overflow: hidden;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 4px;
}

.preview-name {
  margin-right: auto;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}
</style>
