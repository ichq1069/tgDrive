<!-- src/views/LinkParser.vue -->
<template>
  <div class="link-parser-page">
    <el-card class="parser-card">
      <template #header>
        <div class="card-header">
          <el-icon><Link /></el-icon>
          <span>链接解析</span>
        </div>
      </template>

      <div class="input-section">
        <el-form :model="form" label-position="top">
          <el-form-item label="粘贴链接">
            <el-input
              v-model="form.url"
              placeholder="粘贴社交媒体链接，自动识别平台..."
              clearable
              @keydown.enter="handleParse"
              size="large"
            >
              <template #prefix>
                <el-icon><Link /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="平台">
                <el-select v-model="form.platform" placeholder="自动识别" clearable size="default">
                  <el-option label="自动识别" value="" />
                  <el-option label="Instagram" value="instagram" />
                  <el-option label="Twitter/X" value="twitter" />
                  <el-option label="YouTube" value="youtube" />
                  <el-option label="TikTok" value="tiktok" />
                  <el-option label="小红书" value="xiaohongshu" />
                  <el-option label="Pinterest" value="pinterest" />
                  <el-option label="通用" value="generic" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Cookie（可选）">
                <el-input
                  v-model="form.cookie"
                  placeholder="需要登录的站点填入Cookie"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Token（可选）">
                <el-input
                  v-model="form.token"
                  placeholder="API Token"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item>
            <el-button
              type="primary"
              :loading="parsing"
              :disabled="!form.url.trim()"
              @click="handleParse"
              size="large"
            >
              <el-icon v-if="!parsing"><VideoPlay /></el-icon>
              {{ parsing ? '解析中...' : '开始解析' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider v-if="parseResult" />

      <div v-if="parseResult" class="result-section">
        <div v-if="parseResult.error" class="error-box">
          <el-alert :title="parseResult.error" type="error" show-icon :closable="false" />
        </div>

        <div v-else-if="parseResult.items && parseResult.items.length > 0" class="results">
          <div class="results-header">
            <span>解析到 {{ parseResult.items.length }} 个资源</span>
            <el-button type="primary" plain size="small" @click="copyAllLinks">
              <el-icon><CopyDocument /></el-icon> 复制全部链接
            </el-button>
          </div>

          <div class="result-grid">
            <div
              v-for="(item, index) in parseResult.items"
              :key="index"
              class="result-item"
            >
              <div class="item-preview">
                <img
                  v-if="item.thumbnail"
                  :src="item.thumbnail"
                  :alt="item.filename || 'preview'"
                  loading="lazy"
                  @error="handleImageError"
                />
                <div v-else class="no-preview">
                  <el-icon :size="32"><Picture /></el-icon>
                </div>
                <div class="item-type-badge">{{ item.type || 'image' }}</div>
              </div>
              <div class="item-info">
                <div class="item-filename" :title="item.filename || item.url">
                  {{ item.filename || truncateUrl(item.url) }}
                </div>
                <div class="item-url" :title="item.url">{{ item.url }}</div>
                <div class="item-actions">
                  <el-button type="primary" text size="small" @click="copyLink(item.url)">
                    <el-icon><CopyDocument /></el-icon>
                  </el-button>
                  <el-button type="success" text size="small" @click="downloadItem(item)">
                    <el-icon><Download /></el-icon>
                  </el-button>
                  <el-button type="info" text size="small" @click="openInNewTab(item.url)">
                    <el-icon><View /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else description="未解析到资源" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, Picture, CopyDocument, Download, View, VideoPlay } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ParseItem {
  url: string
  thumbnail?: string
  filename?: string
  type?: string
}

interface ParseResult {
  items: ParseItem[]
  error?: string
  title?: string
  author?: string
}

const form = reactive({
  url: '',
  platform: '',
  cookie: '',
  token: '',
})

const parsing = ref(false)
const parseResult = ref<ParseResult | null>(null)

const detectPlatform = (url: string): string => {
  if (/instagram\.com/i.test(url)) return 'instagram'
  if (/twitter\.com|x\.com/i.test(url)) return 'twitter'
  if (/youtube\.com|youtu\.be/i.test(url)) return 'youtube'
  if (/tiktok\.com/i.test(url)) return 'tiktok'
  if (/xiaohongshu\.com|xhslink\.com/i.test(url)) return 'xiaohongshu'
  if (/pinterest\.com/i.test(url)) return 'pinterest'
  return 'generic'
}

const handleParse = async () => {
  const url = form.url.trim()
  if (!url) {
    ElMessage.warning('请输入链接')
    return
  }

  parsing.value = true
  parseResult.value = null

  const platform = form.platform || detectPlatform(url)

  try {
    const response = await request.post('/link-parser/parse', {
      url,
      platform,
      cookie: form.cookie || undefined,
      token: form.token || undefined,
    })

    if (response.data.code === 1) {
      parseResult.value = response.data.data
      if (parseResult.value?.items?.length === 0) {
        ElMessage.info('未解析到资源')
      } else {
        ElMessage.success(`解析到 ${parseResult.value?.items?.length || 0} 个资源`)
      }
    } else {
      parseResult.value = { items: [], error: response.data.message || '解析失败' }
    }
  } catch (err: any) {
    const msg = err?.response?.data?.message || err.message || '解析失败'
    parseResult.value = { items: [], error: msg }
    ElMessage.error(msg)
  } finally {
    parsing.value = false
  }
}

const copyLink = async (url: string) => {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败')
  }
}

const copyAllLinks = async () => {
  if (!parseResult.value?.items?.length) return
  const allLinks = parseResult.value.items.map(i => i.url).join('\n')
  try {
    await navigator.clipboard.writeText(allLinks)
    ElMessage.success(`已复制 ${parseResult.value.items.length} 个链接`)
  } catch {
    ElMessage.error('复制失败')
  }
}

const downloadItem = (item: ParseItem) => {
  const a = document.createElement('a')
  a.href = item.url
  a.target = '_blank'
  a.download = item.filename || ''
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

const openInNewTab = (url: string) => {
  window.open(url, '_blank')
}

const truncateUrl = (url: string) => {
  try {
    const u = new URL(url)
    const path = u.pathname.length > 40 ? u.pathname.slice(0, 40) + '...' : u.pathname
    return u.hostname + path
  } catch {
    return url.length > 60 ? url.slice(0, 60) + '...' : url
  }
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<style scoped>
.link-parser-page {
  max-width: 900px;
  margin: 0 auto;
}

.parser-card :deep(.el-card__header) {
  padding: 16px 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.input-section {
  padding: 0;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.result-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  transition: border-color 0.2s;
}

.result-item:hover {
  border-color: var(--el-color-primary);
}

.item-preview {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: var(--el-fill-color-light);
}

.item-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
}

.item-type-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-filename {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-url {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-actions {
  display: flex;
  gap: 2px;
  margin-top: auto;
}

.error-box {
  margin-bottom: 16px;
}
</style>
