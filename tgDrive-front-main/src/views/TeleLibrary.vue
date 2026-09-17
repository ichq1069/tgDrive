<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Box /></el-icon>
            <span>Tele 库</span>
          </div>
          <div class="header-right">
            <el-input v-model="searchQuery" placeholder="搜索文件名" clearable style="width: 200px; margin-right: 10px;" @keyup.enter="fetchFiles" />
            <el-button type="primary" @click="fetchFiles" :icon="Search">搜索</el-button>
            <el-button type="default" @click="searchQuery = ''; fetchFiles()" :icon="Refresh">刷新</el-button>
            <ViewToggle v-model="viewMode" style="margin-left: 10px;" />
          </div>
        </div>
      </template>

      <!-- 表格视图 -->
      <el-table
        v-if="viewMode === 'table'"
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
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center">
          <template #default="scope">
            {{ formatTime(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" prop="uploader" label="上传者" width="120" align="center" />
        <el-table-column label="操作" width="350" align="center" fixed="right">
          <template #default="scope">
            <el-button-group>
              <el-button type="primary" size="small" @click="copyLink(scope.row)">链接</el-button>
              <el-button type="warning" size="small" @click="downloadFile(scope.row)">下载</el-button>
              <el-button v-if="isAdmin" type="success" size="small" @click="transferToLibrary(scope.row.fileId, 'shared')">转共享</el-button>
              <el-button v-if="isAdmin" type="danger" size="small" @click="transferToLibrary(scope.row.fileId, 'private')">转私密</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 网格视图 -->
      <FileGrid
        v-else
        :files="fileList"
        :selectable="isAdmin"
        :selected-ids="selectedFiles.map(f => f.fileId)"
        v-loading="loading"
        @click="downloadFile"
        @select="toggleSelect"
      >
        <template #actions="{ file }">
          <el-button type="primary" size="small" @click.stop="copyLink(file)" circle :icon="Link" />
          <el-button type="warning" size="small" @click.stop="downloadFile(file)" circle :icon="Download" />
          <el-button v-if="isAdmin" type="success" size="small" @click.stop="transferToLibrary(file.fileId, 'shared')" circle>转共享</el-button>
          <el-button v-if="isAdmin" type="danger" size="small" @click.stop="transferToLibrary(file.fileId, 'private')" circle>转私密</el-button>
        </template>
      </FileGrid>

      <div class="footer-toolbar">
        <div v-if="isAdmin" class="batch-actions">
          <el-button type="success" @click="batchTransfer('shared')" :disabled="selectedFiles.length === 0">批量转共享库</el-button>
          <el-button type="danger" @click="batchTransfer('private')" :disabled="selectedFiles.length === 0">批量转私密库</el-button>
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

    <el-dialog v-model="contentLevelDialogVisible" title="转入私密库" width="400px">
      <el-form label-width="100px">
        <el-form-item label="内容等级">
          <el-select v-model="contentLevel" placeholder="请选择内容等级" style="width: 100%">
            <el-option label="PT" value="pt" />
            <el-option label="VIP" value="vip" />
            <el-option label="SVIP" value="svip" />
            <el-option label="VVIP" value="vvip" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contentLevelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTransfer" :loading="transferLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Search, Refresh, Document, Link, Download } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import ViewToggle from '@/components/ViewToggle.vue'
import FileGrid from '@/components/FileGrid.vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')

const fileList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const searchQuery = ref('')
const selectedFiles = ref<any[]>([])
const contentLevelDialogVisible = ref(false)
const contentLevel = ref('pt')
const transferLoading = ref(false)
const pendingTransferFileIds = ref<string[]>([])
const viewMode = ref<'grid' | 'table'>('table')

const fetchFiles = async () => {
  loading.value = true
  try {
    let url = `/libraries/tele/files?page=${currentPage.value}&size=${pageSize.value}`
    if (searchQuery.value) url += `&keyword=${encodeURIComponent(searchQuery.value)}`
    const res = await request.get(url)
    if (res.data?.code === 1) {
      fileList.value = res.data.data.records || []
      totalItems.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: any[]) => {
  selectedFiles.value = selection
}

const toggleSelect = (file: any) => {
  const index = selectedFiles.value.findIndex(f => f.fileId === file.fileId)
  if (index > -1) {
    selectedFiles.value.splice(index, 1)
  } else {
    selectedFiles.value.push(file)
  }
}

const formatTime = (ts: number) => new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false })

const copyLink = (row: any) => {
  navigator.clipboard.writeText(row.downloadUrl).then(() => ElMessage.success('链接已复制'))
}

const downloadFile = (row: any) => {
  window.open(row.downloadUrl, '_blank')
}

const transferToLibrary = async (fileId: string, target: string) => {
  if (target === 'private') {
    pendingTransferFileIds.value = [fileId]
    contentLevelDialogVisible.value = true
    return
  }
  try {
    const res = await request.post('/libraries/transfer', { fileIds: [fileId], targetLibrary: target })
    if (res.data?.code === 1) {
      ElMessage.success('转入成功')
      fetchFiles()
    } else {
      ElMessage.error(res.data?.msg || '转入失败')
    }
  } catch (e) {
    ElMessage.error('转入失败')
  }
}

const confirmTransfer = async () => {
  transferLoading.value = true
  try {
    const res = await request.post('/libraries/transfer', {
      fileIds: pendingTransferFileIds.value,
      targetLibrary: 'private',
      contentLevel: contentLevel.value
    })
    if (res.data?.code === 1) {
      ElMessage.success('转入成功')
      contentLevelDialogVisible.value = false
      fetchFiles()
    } else {
      ElMessage.error(res.data?.msg || '转入失败')
    }
  } catch (e) {
    ElMessage.error('转入失败')
  } finally {
    transferLoading.value = false
  }
}

const batchTransfer = async (target: string) => {
  if (target === 'private') {
    pendingTransferFileIds.value = selectedFiles.value.map(f => f.fileId)
    contentLevelDialogVisible.value = true
    return
  }
  try {
    const res = await request.post('/libraries/transfer', {
      fileIds: selectedFiles.value.map(f => f.fileId),
      targetLibrary: target
    })
    if (res.data?.code === 1) {
      ElMessage.success('批量转入成功')
      selectedFiles.value = []
      fetchFiles()
    } else {
      ElMessage.error(res.data?.msg || '转入失败')
    }
  } catch (e) {
    ElMessage.error('转入失败')
  }
}

onMounted(() => fetchFiles())
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

.footer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 10px;
}

.batch-actions {
  display: flex;
  gap: 10px;
}
</style>
