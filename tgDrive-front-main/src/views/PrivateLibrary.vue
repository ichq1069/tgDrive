<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Lock /></el-icon>
            <span>私密库</span>
          </div>
          <div class="header-right">
            <el-input v-model="searchQuery" placeholder="搜索文件名" clearable style="width: 200px; margin-right: 10px;" @keyup.enter="fetchFiles" />
            <el-button type="primary" @click="fetchFiles" :icon="Search">搜索</el-button>
            <el-button type="default" @click="searchQuery = ''; fetchFiles()" :icon="Refresh">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="fileList"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        height="calc(100vh - 280px)"
        style="width: 100%;"
      >
        <el-table-column type="selection" width="55" align="center" v-if="isAdmin" />
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon><Document /></el-icon>
              <span>{{ scope.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="120" align="center" />
        <el-table-column prop="contentLevel" label="等级" width="80" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center">
          <template #default="scope">
            {{ formatTime(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="scope">
            <el-button-group>
              <el-button type="primary" size="small" @click="copyLink(scope.row)">链接</el-button>
              <el-button type="warning" size="small" @click="downloadFile(scope.row)">下载</el-button>
              <el-button v-if="isAdmin" type="info" size="small" @click="restoreToTele(scope.row.fileId)">转回Tele</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <div class="footer-toolbar">
        <div v-if="isAdmin" class="batch-actions">
          <el-button type="info" @click="batchRestore" :disabled="selectedFiles.length === 0">批量转回 Tele</el-button>
        </div>
        <el-pagination
          v-if="totalItems > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalItems"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchFiles"
          @current-change="fetchFiles"
          background
          small
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Search, Refresh, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')

const fileList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const searchQuery = ref('')
const selectedFiles = ref<any[]>([])

const fetchFiles = async () => {
  loading.value = true
  try {
    let url = `/libraries/private/files?page=${currentPage.value}&size=${pageSize.value}`
    if (searchQuery.value) url += `&keyword=${encodeURIComponent(searchQuery.value)}`
    const res = await request.get(url)
    if (res.data?.code === 1) {
      fileList.value = res.data.data.records || []
      totalItems.value = res.data.data.total || 0
    }
  } catch (e: any) {
    if (e?.response?.status === 403) {
      ElMessage.error('无权访问私密库')
    } else if (e?.response?.status === 401) {
      ElMessage.error('请先登录')
    } else {
      ElMessage.error('获取文件列表失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: any[]) => {
  selectedFiles.value = selection
}

const formatTime = (ts: number) => new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false })

const copyLink = (row: any) => {
  navigator.clipboard.writeText(row.downloadUrl).then(() => ElMessage.success('链接已复制'))
}

const downloadFile = (row: any) => {
  window.open(row.downloadUrl, '_blank')
}

const restoreToTele = async (fileId: string) => {
  try {
    const res = await request.post('/libraries/restore', { fileIds: [fileId] })
    if (res.data?.code === 1) {
      ElMessage.success('转回成功')
      fetchFiles()
    }
  } catch (e) {
    ElMessage.error('转回失败')
  }
}

const batchRestore = async () => {
  try {
    const res = await request.post('/libraries/restore', {
      fileIds: selectedFiles.value.map(f => f.fileId)
    })
    if (res.data?.code === 1) {
      ElMessage.success('批量转回成功')
      selectedFiles.value = []
      fetchFiles()
    }
  } catch (e) {
    ElMessage.error('批量转回失败')
  }
}

onMounted(() => fetchFiles())
</script>
