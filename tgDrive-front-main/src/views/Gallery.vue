<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Picture /></el-icon>
            <span>随机画廊</span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="fetchRandom" :loading="loading">换一批</el-button>
            <el-input v-model="searchQuery" placeholder="关键词过滤" clearable style="width: 150px; margin-left: 10px;" @keyup.enter="fetchRandom" />
            <ViewToggle v-model="viewMode" style="margin-left: 10px;" />
          </div>
        </div>
      </template>

      <div v-if="loading" style="display:flex;align-items:center;justify-content:center;height:400px;">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      </div>
      <div v-else-if="galleryList.length === 0" style="display:flex;align-items:center;justify-content:center;height:400px;color:#999;">
        <span>暂无随机池数据</span>
      </div>

      <!-- 网格视图 -->
      <div v-else-if="viewMode === 'grid'" class="gallery-grid">
        <div v-for="item in galleryList" :key="item.fileId" class="gallery-card" @click="openFile(item)">
          <div class="gallery-thumb">
            <el-icon :size="48"><Document /></el-icon>
          </div>
          <div class="gallery-info">
            <div class="gallery-name" :title="item.fileName">{{ item.fileName }}</div>
            <div class="gallery-meta">
              <span>{{ item.size }}</span>
              <span v-if="item.tags" class="gallery-tags">
                <el-tag v-for="tag in parseTags(item.tags)" :key="tag" size="small" type="info" style="margin-left: 4px;">{{ tag }}</el-tag>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 表格视图 -->
      <el-table
        v-else
        :data="galleryList"
        height="calc(100vh - 280px)"
        style="width: 100%;"
      >
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon><Document /></el-icon>
              <span>{{ scope.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="120" align="center" />
        <el-table-column label="标签" width="200" align="center">
          <template #default="scope">
            <el-tag v-for="tag in parseTags(scope.row.tags)" :key="tag" size="small" type="info" style="margin: 2px;">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openFile(scope.row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, Loading, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import ViewToggle from '@/components/ViewToggle.vue'

const galleryList = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const viewMode = ref<'grid' | 'table'>('grid')

const fetchRandom = async () => {
  loading.value = true
  try {
    let url = '/gallery?page=1&size=20'
    if (searchQuery.value) url += `&keyword=${encodeURIComponent(searchQuery.value)}`
    const res = await request.get(url)
    if (res.data?.code === 1) {
      galleryList.value = res.data.data?.records || []
    }
  } catch (e) {
    ElMessage.error('获取随机画廊失败')
  } finally {
    loading.value = false
  }
}

const parseTags = (tags: any) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.map((t: any) => t.name || t)
  try {
    return JSON.parse(tags).map((t: any) => t.name || t)
  } catch { return [] }
}

const openFile = (item: any) => {
  window.open(item.downloadUrl, '_blank')
}

onMounted(() => fetchRandom())
</script>

<style scoped>
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 16px;
}
.gallery-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  text-align: center;
}
.gallery-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
}
.gallery-thumb {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}
.gallery-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}
.gallery-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.gallery-tags {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
}
</style>
