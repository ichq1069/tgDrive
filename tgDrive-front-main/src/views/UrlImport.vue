<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Link /></el-icon>
            <span>URL 导入</span>
          </div>
        </div>
      </template>

      <div class="import-form">
        <el-input
          v-model="urlText"
          type="textarea"
          :rows="10"
          placeholder="输入URL，每行一个&#10;&#10;示例：&#10;https://example.com/file1.zip&#10;https://example.com/file2.pdf"
          :disabled="importing"
        />
        <div class="form-actions">
          <el-button @click="clearInput" :disabled="importing">清空</el-button>
          <el-button type="primary" @click="startImport" :loading="importing" :disabled="!urlText.trim()">
            {{ importing ? '导入中...' : '开始导入' }}
          </el-button>
        </div>
      </div>

      <!-- 进度区域 -->
      <div v-if="importStarted" class="progress-section">
        <el-divider content-position="left">导入进度</el-divider>

        <!-- 总体进度 -->
        <div class="progress-overview">
          <div class="progress-stats">
            <span class="stat total">总计: {{ progressData.total }}</span>
            <span class="stat success">成功: {{ progressData.completed }}</span>
            <span class="stat fail">失败: {{ progressData.failed }}</span>
            <span class="stat pending">剩余: {{ progressData.total - progressData.completed - progressData.failed }}</span>
          </div>
          <el-progress
            :percentage="overallPercentage"
            :status="overallStatus"
            :stroke-width="20"
            text-inside
          />
        </div>

        <!-- 当前文件进度 -->
        <div v-if="currentFile" class="current-file">
          <div class="file-info">
            <el-icon v-if="currentFile.status === 'downloading'" class="is-loading"><Download /></el-icon>
            <el-icon v-else-if="currentFile.status === 'uploading'" class="is-loading"><Upload /></el-icon>
            <el-icon v-else-if="currentFile.status === 'completed'" color="#67c23a"><CircleCheck /></el-icon>
            <el-icon v-else-if="currentFile.status === 'failed'" color="#f56c6c"><CircleClose /></el-icon>
            <span class="file-name">{{ currentFile.fileName }}</span>
            <el-tag :type="getStatusType(currentFile.status)" size="small">{{ getStatusText(currentFile.status) }}</el-tag>
          </div>
          <div class="file-index">({{ currentFile.currentIndex }}/{{ currentFile.total }})</div>
        </div>

        <!-- 失败列表 -->
        <div v-if="failedUrls.length > 0" class="failed-section">
          <el-collapse>
            <el-collapse-item>
              <template #title>
                <span style="color: var(--el-color-danger)">失败链接 ({{ failedUrls.length }})</span>
              </template>
              <div class="failed-list">
                <div v-for="(url, idx) in failedUrls" :key="idx" class="failed-item">
                  {{ url }}
                </div>
              </div>
              <el-button type="primary" size="small" style="margin-top: 10px" @click="retryFailed">
                重试失败链接
              </el-button>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, Download, Upload, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import request from '@/utils/request'

const urlText = ref('')
const importing = ref(false)
const importStarted = ref(false)
const currentFile = ref<any>(null)
const failedUrls = ref<string[]>([])
const progressData = ref({ total: 0, completed: 0, failed: 0 })

let ws: WebSocket | null = null

const overallPercentage = computed(() => {
  if (progressData.value.total === 0) return 0
  return Math.round(((progressData.value.completed + progressData.value.failed) / progressData.value.total) * 100)
})

const overallStatus = computed(() => {
  if (progressData.value.failed > 0) return 'exception'
  if (progressData.value.completed === progressData.value.total && progressData.value.total > 0) return 'success'
  return ''
})

const getStatusType = (status: string) => {
  switch (status) {
    case 'downloading': return 'warning'
    case 'uploading': return ''
    case 'completed': return 'success'
    case 'failed': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'downloading': return '下载中'
    case 'uploading': return '上传中'
    case 'completed': return '完成'
    case 'failed': return '失败'
    default: return status
  }
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws/upload-progress`
  ws = new WebSocket(wsUrl)

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)

      if (msg.type === 'import_start') {
        importStarted.value = true
        progressData.value = { total: msg.total, completed: 0, failed: 0 }
        currentFile.value = null
        failedUrls.value = []
      }

      if (msg.type === 'import_progress') {
        currentFile.value = {
          fileName: msg.fileName,
          currentIndex: msg.currentIndex,
          total: msg.total,
          status: msg.status
        }
        progressData.value.completed = msg.completed
        progressData.value.failed = msg.failed
      }

      if (msg.type === 'import_complete') {
        importing.value = false
        progressData.value.completed = msg.completed
        progressData.value.failed = msg.failed
        failedUrls.value = msg.failedUrls || []

        if (msg.failed === 0) {
          ElMessage.success(`全部导入成功！共 ${msg.completed} 个文件`)
        } else {
          ElMessage.warning(`导入完成：成功 ${msg.completed} 个，失败 ${msg.failed} 个`)
        }
      }
    } catch (e) {
      // ignore parse errors
    }
  }

  ws.onclose = () => {
    // 3秒后重连
    setTimeout(connectWebSocket, 3000)
  }

  ws.onerror = () => {
    ws?.close()
  }
}

const startImport = async () => {
  const urls = urlText.value
    .split('\n')
    .map(u => u.trim())
    .filter(u => u.length > 0 && (u.startsWith('http://') || u.startsWith('https://')))

  if (urls.length === 0) {
    ElMessage.warning('请输入有效的URL（以 http:// 或 https:// 开头）')
    return
  }

  importing.value = true
  importStarted.value = true
  progressData.value = { total: urls.length, completed: 0, failed: 0 }
  currentFile.value = null
  failedUrls.value = []

  try {
    const res = await request.post('/import/url', { urls })
    if (res.data?.code !== 1) {
      ElMessage.error(res.data?.msg || '提交导入任务失败')
      importing.value = false
    }
  } catch (e) {
    ElMessage.error('提交导入任务失败')
    importing.value = false
  }
}

const clearInput = () => {
  urlText.value = ''
  importStarted.value = false
  currentFile.value = null
  failedUrls.value = []
  progressData.value = { total: 0, completed: 0, failed: 0 }
}

const retryFailed = () => {
  if (failedUrls.value.length === 0) return
  urlText.value = failedUrls.value.join('\n')
  failedUrls.value = []
  startImport()
}

onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  ws?.close()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
}

.content-card {
  height: 100%;
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

.import-form {
  max-width: 800px;
  margin: 0 auto;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.progress-section {
  max-width: 800px;
  margin: 30px auto 0;
}

.progress-overview {
  margin-bottom: 20px;
}

.progress-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 14px;
}

.stat.total { color: var(--el-text-color-regular); }
.stat.success { color: var(--el-color-success); }
.stat.fail { color: var(--el-color-danger); }
.stat.pending { color: var(--el-text-color-secondary); }

.current-file {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-top: 16px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-index {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  flex-shrink: 0;
  margin-left: 12px;
}

.failed-section {
  margin-top: 20px;
}

.failed-list {
  max-height: 200px;
  overflow-y: auto;
}

.failed-item {
  padding: 4px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
}
</style>
