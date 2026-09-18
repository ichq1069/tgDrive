<template>
  <div class="web-scrape-container">
    <el-card class="scrape-card">
      <template #header>
        <div class="card-header">
          <el-icon><Link /></el-icon>
          <span>网页图片提取</span>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <el-form-item label="网页地址" required>
          <el-input
            v-model="form.url"
            placeholder="请输入网页地址 (https://example.com/image-page)"
            :disabled="parsing"
          />
        </el-form-item>

        <el-form-item label="Cookie（可选）">
          <el-input
            v-model="form.cookie"
            type="textarea"
            :rows="2"
            placeholder="如果网页需要登录或有反爬保护，请填写Cookie"
            :disabled="parsing"
          />
        </el-form-item>

        <el-form-item label="浏览器模式">
          <el-switch
            v-model="form.browserMode"
            active-text="使用浏览器（可绕过反爬，较慢）"
            inactive-text="快速模式"
            :disabled="parsing"
          />
          <div class="form-tip">开启后使用真实浏览器引擎解析，可处理JS渲染和反爬保护，但速度较慢</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleParse" :loading="parsing" :disabled="!form.url">
            <el-icon><Search /></el-icon>
            解析网页
          </el-button>
          <el-button v-if="parsed" type="success" @click="handleImportSelected" :loading="importing" :disabled="selectedImages.length === 0">
            <el-icon><Download /></el-icon>
            导入选中 ({{ selectedImages.length }})
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 解析结果 -->
    <el-card v-if="parsed" class="result-card">
      <template #header>
        <div class="card-header">
          <span>解析结果</span>
          <el-tag type="info">{{ pageInfo.title }}</el-tag>
          <el-tag>共 {{ images.length }} 张图片</el-tag>
          <el-button size="small" @click="toggleSelectAll">
            {{ allSelected ? '取消全选' : '全选' }}
          </el-button>
        </div>
      </template>

      <div class="image-grid">
        <div
          v-for="(image, index) in images"
          :key="index"
          class="image-item"
          :class="{ selected: selectedImages.includes(index) }"
          @click="toggleSelect(index)"
        >
          <el-checkbox
            :model-value="selectedImages.includes(index)"
            class="image-checkbox"
            @click.stop
            @change="toggleSelect(index)"
          />
          <img
            :src="image.url"
            :alt="image.alt"
            @error="handleImageError($event)"
            loading="lazy"
          />
          <div class="image-url" :title="image.url">
            {{ truncateUrl(image.url) }}
          </div>
        </div>
      </div>

      <div v-if="images.length === 0" class="no-images">
        <el-empty description="未找到图片" />
      </div>
    </el-card>

    <!-- 导入进度 -->
    <el-card v-if="importing || importResult" class="progress-card">
      <template #header>
        <div class="card-header">
          <el-icon><Loading v-if="importing" /></el-icon>
          <span>导入进度</span>
        </div>
      </template>

      <div v-if="importing" class="progress-content">
        <el-progress
          :percentage="Math.round((importProgress.completed / importProgress.total) * 100)"
          :status="importProgress.failed > 0 ? 'exception' : ''"
        />
        <div class="progress-stats">
          <el-tag type="success">成功: {{ importProgress.completed }}</el-tag>
          <el-tag type="danger" v-if="importProgress.failed > 0">失败: {{ importProgress.failed }}</el-tag>
          <el-tag type="info">总计: {{ importProgress.total }}</el-tag>
        </div>
      </div>

      <div v-if="importResult" class="import-result">
        <el-alert
          v-if="importResult.success"
          :title="`导入完成: 成功 ${importResult.completed} 个，失败 ${importResult.failed} 个`"
          type="success"
          show-icon
        />
        <el-alert
          v-else
          :title="`导入失败: ${importResult.message}`"
          type="error"
          show-icon
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, Search, Download, Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'

const form = ref({
  url: '',
  cookie: '',
  browserMode: false
})

const parsing = ref(false)
const parsed = ref(false)
const importing = ref(false)
const images = ref<Array<{ url: string; alt: string }>>([])
const selectedImages = ref<number[]>([])
const pageInfo = ref({ title: '' })

const importProgress = ref({
  total: 0,
  completed: 0,
  failed: 0
})

const importResult = ref<{
  success: boolean;
  completed: number;
  failed: number;
  message?: string;
} | null>(null)

let ws: WebSocket | null = null

const allSelected = computed(() => {
  return images.value.length > 0 && selectedImages.value.length === images.value.length
})

const handleParse = async () => {
  if (!form.value.url) {
    ElMessage.warning('请输入网页地址')
    return
  }

  parsing.value = true
  parsed.value = false
  images.value = []
  selectedImages.value = []

  try {
    const response = await request.post('/import/parse-page', {
      url: form.value.url,
      cookie: form.value.cookie || undefined,
      browserMode: form.value.browserMode ? 'true' : undefined
    })

    if (response.data.code === 1) {
      const result = response.data.data
      pageInfo.value = { title: result.title || '未知页面' }
      images.value = result.images || []
      parsed.value = true
      ElMessage.success(`解析完成，找到 ${images.value.length} 张图片`)
    } else {
      ElMessage.error(response.data.message || '解析失败')
    }
  } catch (error: any) {
    ElMessage.error('解析失败: ' + (error.response?.data?.message || error.message))
  } finally {
    parsing.value = false
  }
}

const toggleSelect = (index: number) => {
  const idx = selectedImages.value.indexOf(index)
  if (idx === -1) {
    selectedImages.value.push(index)
  } else {
    selectedImages.value.splice(idx, 1)
  }
}

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedImages.value = []
  } else {
    selectedImages.value = images.value.map((_, i) => i)
  }
}

const truncateUrl = (url: string) => {
  if (url.length > 50) {
    return url.substring(0, 50) + '...'
  }
  return url
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

const handleImportSelected = async () => {
  if (selectedImages.value.length === 0) {
    ElMessage.warning('请先选择要导入的图片')
    return
  }

  importing.value = true
  importResult.value = null

  const selectedUrls = selectedImages.value.map(i => images.value[i].url)

  try {
    // 建立WebSocket连接接收进度
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/import-progress`
    ws = new WebSocket(wsUrl)

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'import_start') {
          importProgress.value = {
            total: data.total,
            completed: 0,
            failed: 0
          }
        } else if (data.type === 'import_progress') {
          importProgress.value.completed = data.completed
          importProgress.value.failed = data.failed
        } else if (data.type === 'import_complete') {
          importResult.value = {
            success: true,
            completed: data.completed,
            failed: data.failed
          }
          importing.value = false
          ElMessage.success(`导入完成: 成功 ${data.completed} 个，失败 ${data.failed} 个`)
          if (ws) {
            ws.close()
            ws = null
          }
        }
      } catch (e) {
        console.error('WebSocket消息解析失败:', e)
      }
    }

    ws.onerror = () => {
      ElMessage.warning('WebSocket连接失败，将继续导入但无法获取实时进度')
    }

    // 发送导入请求
    const response = await request.post('/import/parse-and-import', {
      url: form.value.url,
      imageUrls: selectedUrls
    })

    if (response.data.code !== 1) {
      ElMessage.error(response.data.message || '导入请求失败')
      importing.value = false
    }
  } catch (error: any) {
    ElMessage.error('导入失败: ' + (error.response?.data?.message || error.message))
    importing.value = false
  }
}

onBeforeUnmount(() => {
  if (ws) {
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.web-scrape-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.scrape-card,
.result-card,
.progress-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.image-item {
  position: relative;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.image-item:hover {
  border-color: #409eff;
}

.image-item.selected {
  border-color: #67c23a;
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
}

.image-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 1;
}

.image-item img {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

.image-url {
  padding: 8px;
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-images {
  padding: 40px 0;
}

.progress-content {
  text-align: center;
}

.progress-stats {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.import-result {
  margin-top: 16px;
}
</style>
