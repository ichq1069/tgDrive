<template>
  <div class="web-scrape-container">
    <!-- URL Input Card -->
    <el-card class="scrape-card">
      <div class="input-group">
        <el-input
          v-model="form.url"
          placeholder="粘贴网页链接，例如 https://example.com/post/123"
          :disabled="parsing"
          @keydown.enter="handleParse"
        />
        <el-button @click="form.url = ''" :disabled="parsing" title="清空输入框">✕</el-button>
        <el-button @click="handlePaste" :disabled="parsing" title="粘贴剪贴板">📋</el-button>
        <el-button type="primary" @click="handleParse" :loading="parsing" :disabled="!form.url">拾取图片</el-button>
      </div>
      <div class="status" :class="statusClass">{{ statusText }}</div>
      <div style="display:flex;align-items:center;gap:12px;margin-top:8px">
        <el-switch v-model="form.browserMode" active-text="浏览器模式（绕过Cloudflare）" inactive-text="快速模式" :disabled="parsing" />
        <el-radio-group v-model="form.deviceMode" size="small" :disabled="!form.browserMode || parsing" v-if="form.browserMode">
          <el-radio-button value="pc">PC桌面</el-radio-button>
          <el-radio-button value="h5">H5手机</el-radio-button>
        </el-radio-group>
        <div class="form-tip">{{ form.browserMode ? (form.deviceMode === 'h5' ? '模拟手机访问，获取移动端图片' : '模拟桌面访问，获取全尺寸图片') : '开启后使用真实浏览器解析，可绕过反爬保护' }}</div>
      </div>
      <div class="form-tip">整页解析被拦截时，可直接粘贴多条图片直链（每行一条），或粘贴 &lt;img data-src/src&gt; 片段，工具会自动抽取图片作为候选</div>
    </el-card>

    <!-- Candidate Card -->
    <el-card v-if="parsed || cands.length > 0" class="cand-card">
      <!-- Options Row -->
      <div class="opts-row">
        <div class="field" style="flex:2;min-width:200px">
          <label>标题（可选，默认用页面标题）</label>
          <el-input v-model="pageTitle" placeholder="默认取网页 title" />
        </div>
        <div class="field" style="flex:2;min-width:220px">
          <label>标签（从后台标签库多选）</label>
          <div style="display:flex;gap:8px;align-items:center">
            <el-input v-model="selectedTagsText" readonly placeholder="点击「选择标签」勾选" @click="showTagPicker = true" />
            <el-button @click="showTagPicker = true" style="white-space:nowrap">选择标签…</el-button>
          </div>
        </div>
        <div class="field" style="flex:0 0 140px">
          <label>入库级别</label>
          <el-select v-model="contentLevel" style="width:100%">
            <el-option label="pt" value="pt" />
            <el-option label="vip" value="vip" />
            <el-option label="svip" value="svip" />
            <el-option label="vvip" value="vvip" />
          </el-select>
        </div>
      </div>

      <!-- Rule Group Row -->
      <div class="ign-row">
        <span class="ig-label">规则组</span>
        <el-select v-model="activeGroupKey" class="group-select" @change="handleGroupSwitch">
          <el-option v-for="g in ruleGroups" :key="g.key" :label="g.name || groupKeyName(g.key)" :value="g.key" />
        </el-select>
        <el-button @click="bindCurrentDomain" title="把当前规则另存为只对该域名生效的规则组">绑定本页域名</el-button>
        <el-button @click="createNewGroup" title="基于当前规则新建一套规则组">新建规则组…</el-button>
      </div>

      <!-- Ignore Rules Row -->
      <div class="ign-row">
        <span class="ig-label">忽略内容</span>
        <el-input v-model="currentGroup.kw" placeholder="链接含以下内容则忽略（逗号分隔，如 logo,icon,avatar）" @change="saveGroups" />
        <el-input v-model="currentGroup.ext" placeholder="忽略格式（如 svg,bmp,ico）" style="flex:0 0 140px" @change="saveGroups" />
        <el-input-number v-model="currentGroup.mb" :min="1" :max="30" controls-position="right" style="flex:0 0 110px" @change="saveGroups" />
        <span class="rules-hint" v-if="rulesHintText">{{ rulesHintText }}</span>
      </div>

      <!-- Required Content Row -->
      <div class="ign-row">
        <span class="ig-label">必带内容</span>
        <el-input v-model="currentGroup.must" placeholder="链接必须含以下内容才保留（逗号分隔，如 xhscdn；留空=不限）" @change="saveGroups" />
        <span class="form-tip" style="flex:0 0 auto">与「忽略内容」互逆</span>
      </div>

      <!-- Default Level & Tags Row -->
      <div class="ign-row">
        <span class="ig-label">默认入库</span>
        <el-select v-model="currentGroup.defaultLevel" style="flex:0 0 120px" @change="saveGroups">
          <el-option label="不设置" value="" />
          <el-option label="pt" value="pt" />
          <el-option label="vip" value="vip" />
          <el-option label="svip" value="svip" />
          <el-option label="vvip" value="vvip" />
        </el-select>
        <el-input v-model="currentGroup.defaultTags" placeholder="默认标签（逗号分隔，拾取时自动填入）" @change="saveGroups" />
      </div>

      <!-- Tag Regex Row -->
      <div class="ign-row">
        <span class="ig-label">标题提词</span>
        <el-input v-model="currentGroup.tagRegex" placeholder="正则：从页面标题提取关键词追加到标签（如 ([\\u4e00-\\u9fa5]{2,6})）" @change="saveGroups" />
        <span class="form-tip" style="flex:0 0 auto">留空不启用</span>
      </div>

      <!-- Sniffing Rules: CSS Selector -->
      <div class="ign-row">
        <span class="ig-label">嗅探范围</span>
        <el-input v-model="currentGroup.cssSelector" placeholder="CSS选择器：限定嗅探范围（如 .gallery, #content, .post-body）" @change="saveGroups" />
        <span class="form-tip" style="flex:0 0 auto">留空=全页嗅探</span>
      </div>

      <!-- Cookie Row -->
      <div class="ign-row">
        <span class="ig-label">站点Cookie</span>
        <el-input v-model="siteCookie" type="password" show-password placeholder="粘贴该站登录 Cookie（F12 → Network 请求头复制）" />
        <el-button @click="clearSiteCookie">清除</el-button>
        <span class="form-tip" v-if="cookieHint">{{ cookieHint }}</span>
      </div>

      <!-- Toolbar -->
      <div class="toolbar">
        <span class="count">共 <b>{{ cands.length }}</b> 个 · 已选 <b>{{ selectedCands.length }}</b> 个</span>
        <el-button text @click="selectAllCands">全选</el-button>
        <el-button text @click="clearSelCands">全不选</el-button>
        <el-button text @click="selectFirst20">只选前20个</el-button>
        <el-button text @click="copySelectedUrls">复制地址</el-button>
        <el-button v-if="showXhsExtractBtn" type="warning" text @click="extractXhsImages" style="color:#ff6a00">
          提取小红书图片
        </el-button>
        <el-button v-if="showXhsExtractBtn" text @click="copyAllXhsUrls" style="color:#ff6a00">
          复制全部ci链接
        </el-button>
        <span style="display:inline-flex;align-items:center;gap:4px;font-size:12px;color:#8a8aa8">
          并发 <el-input-number v-model="concurrency" :min="1" :max="5" controls-position="right" style="width:70px" size="small" /> 个
        </span>
        <el-button type="primary" :disabled="selectedCands.length === 0 || importing" @click="handleBatchImport">
          直传选中（{{ contentLevel }} 级别）
        </el-button>
      </div>

      <!-- Progress -->
      <div v-if="importing || importResult" class="progress-wrap">
        <el-progress
          :percentage="importProgress.total ? Math.round((importProgress.completed / importProgress.total) * 100) : 0"
          :status="importProgress.failed > 0 ? 'exception' : ''"
        />
        <div class="progress-info">
          <span>成功: {{ importProgress.completed }} / {{ importProgress.total }}</span>
          <span v-if="importProgress.failed > 0" style="color:#f56c6c">失败: {{ importProgress.failed }}</span>
          <span v-if="importProgress.currentUrl" class="prog-url">{{ importProgress.currentUrl }}</span>
          <el-button v-if="importing" text @click="togglePause">
            {{ paused ? '继续' : '暂停' }}
          </el-button>
        </div>
      </div>

      <!-- Image/Video Grid -->
      <div class="image-grid">
        <div
          v-for="(cand, index) in cands"
          :key="index"
          class="image-item"
          :class="{ selected: selectedCandIndices.includes(index) }"
        >
          <input
            type="checkbox"
            :checked="selectedCandIndices.includes(index)"
            @change="toggleCand(index)"
            class="image-checkbox"
          />
          <div class="image-preview" @click.stop="openPreview(getCandDisplayUrl(cand))">
            <img v-if="isImageUrl(cand)" :src="cand" loading="lazy" referrerpolicy="no-referrer" @error="handleImageError($event)" alt="" />
            <div v-else class="video-thumb">
              <video :src="cand" preload="metadata" muted @loadeddata="handleVideoLoad($event, index)"></video>
              <div class="video-play-icon">▶</div>
            </div>
            <div class="preview-hint" @click.stop="openPreview(getCandDisplayUrl(cand))">预览</div>
          </div>
          <div class="image-name" :title="getCandDisplayUrl(cand)">
            <template v-if="xhsCandMap.has(cand)">
              <span class="xhs-badge">XHS</span>
              <span class="xhs-converted">{{ truncName(getCandDisplayUrl(cand)) }}</span>
              <span class="xhs-original" :title="cand">{{ truncName(cand) }}</span>
            </template>
            <template v-else>
              {{ truncName(cand) }}
            </template>
          </div>
        </div>
      </div>

      <div v-if="cands.length === 0 && parsed" class="no-images">
        <el-empty description="未找到图片" />
      </div>

      <!-- Import Result -->
      <div v-if="importResult" class="import-result">
        <el-alert
          :title="`导入完成: 成功 ${importResult.completed} 个，失败 ${importResult.failed} 个`"
          :type="importResult.failed > 0 ? 'warning' : 'success'"
          show-icon
        />
      </div>
    </el-card>

    <!-- Tag Picker Dialog -->
    <el-dialog v-model="showTagPicker" title="从后台标签库选择标签" width="600px" :close-on-click-modal="false">
      <el-input v-model="tagSearchKw" placeholder="搜索标签…" style="margin-bottom:12px" />
      <div class="tag-picker-body">
        <span
          v-for="tag in filteredTagLib"
          :key="tag.id"
          class="tchip"
          :class="{ on: pickedTagNames.includes(tag.name) }"
          @click="togglePickTag(tag.name)"
        >
          {{ tag.name }}
        </span>
        <div v-if="filteredTagLib.length === 0" style="color:#8a8aa8;font-size:13px;padding:16px">无匹配标签</div>
      </div>
      <template #footer>
        <el-button @click="pickedTagNames = []; showTagPicker = false">清空</el-button>
        <el-button type="primary" @click="confirmTagPick">确定</el-button>
      </template>
    </el-dialog>

    <!-- Sniffing Floating Window -->
    <Teleport to="body">
      <div v-if="showFloatPanel" class="float-panel" :style="{ left: floatPos.x + 'px', top: floatPos.y + 'px' }" @mousedown="startDrag">
        <div class="float-header">
          <span class="float-title">{{ floatPanelStatus === 'parsing' ? '嗅探中...' : floatPanelStatus === 'done' ? '嗅探完成' : '嗅探失败' }}</span>
          <el-button text size="small" @click.stop="showFloatPanel = false" class="float-close">✕</el-button>
        </div>
        <div class="float-body">
          <div v-if="floatPanelStatus === 'parsing'" class="float-spinner">
            <div class="spinner"></div>
            <span>{{ parseProgress || statusText }}</span>
          </div>
          <div v-else-if="floatPanelStatus === 'done'" class="float-result">
            <span style="color:#67c23a">找到 {{ cands.length }} 个资源</span>
          </div>
          <div v-else class="float-result">
            <span style="color:#f56c6c">{{ parseProgress || '解析失败' }}</span>
          </div>
          <div class="float-log" v-if="floatLogs.length">
            <div v-for="(log, i) in floatLogs" :key="i" class="float-log-item">{{ log }}</div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request, { longTimeoutService } from '@/utils/request'

// ===== Form State =====
const form = ref({ url: '', browserMode: false, deviceMode: 'pc' })
const parsing = ref(false)
const parsed = ref(false)
const pageTitle = ref('')
const contentLevel = ref('pt')
const cands = ref<string[]>([])
const selectedCandIndices = ref<number[]>([])
const importing = ref(false)
const paused = ref(false)
const concurrency = ref(3)
const statusText = ref('')
const statusClass = ref('')

// ===== Parse Progress =====
const parseProgress = ref('')
let parseWs: WebSocket | null = null

// ===== Floating Panel =====
const showFloatPanel = ref(false)
const floatPanelStatus = ref<'parsing' | 'done' | 'error'>('parsing')
const floatLogs = ref<string[]>([])
const floatPos = ref({ x: window.innerWidth - 340, y: 80 })
let isDragging = false
let dragOffset = { x: 0, y: 0 }

function startDrag(e: MouseEvent) {
  isDragging = true
  dragOffset = { x: e.clientX - floatPos.value.x, y: e.clientY - floatPos.value.y }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}
function onDrag(e: MouseEvent) {
  if (isDragging) {
    floatPos.value = { x: Math.max(0, e.clientX - dragOffset.x), y: Math.max(0, e.clientY - dragOffset.y) }
  }
}
function stopDrag() {
  isDragging = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// ===== Import Progress =====
const importProgress = ref({ total: 0, completed: 0, failed: 0, currentUrl: '' })
const importResult = ref<{ completed: number; failed: number } | null>(null)
let ws: WebSocket | null = null
let abortController: AbortController | null = null
let pendingUrls: string[] = []

// ===== Rule Groups =====
interface RuleGroup {
  key: string
  name: string
  kw: string
  ext: string
  mb: number
  must: string
  defaultLevel: string
  defaultTags: string
  tagRegex: string
  cssSelector: string
}
const GROUPS_KEY = 'tgdrive_scrapeRuleGroups'
const ruleGroups = ref<RuleGroup[]>([])
const activeGroupKey = ref('*')
const rulesHintText = ref('')
const rulesHintTimer: ReturnType<typeof setTimeout> | null = null

const currentGroup = computed(() => {
  return ruleGroups.value.find(g => g.key === activeGroupKey.value) || defaultGroupObj('*')
})

function defaultGroupObj(key: string): RuleGroup {
  return { key, name: groupKeyName(key), kw: '', ext: 'svg', mb: 10, must: '', defaultLevel: '', defaultTags: '', tagRegex: '', cssSelector: '' }
}

function groupKeyName(key: string) {
  return key === '*' ? '默认（所有页面）' : key
}

function loadGroups() {
  try {
    const s = localStorage.getItem(GROUPS_KEY)
    if (s) { ruleGroups.value = JSON.parse(s); if (!Array.isArray(ruleGroups.value)) ruleGroups.value = [] }
  } catch { ruleGroups.value = [] }
  if (!ruleGroups.value.length) ruleGroups.value.unshift(defaultGroupObj('*'))
  if (!ruleGroups.value.find(g => g.key === '*')) ruleGroups.value.unshift(defaultGroupObj('*'))
}

function saveGroups() {
  try { localStorage.setItem(GROUPS_KEY, JSON.stringify(ruleGroups.value)) } catch {}
  scheduleCloudSync()
}

let cloudSyncTimer: ReturnType<typeof setTimeout> | null = null
function scheduleCloudSync() {
  clearTimeout(cloudSyncTimer!)
  cloudSyncTimer = setTimeout(pushGroupsToCloud, 1200)
}

async function pushGroupsToCloud() {
  try {
    await request.post('/scrape/rule-groups', { groups: ruleGroups.value })
  } catch {}
}

async function pullGroupsFromCloud() {
  try {
    const res = await request.get('/scrape/rule-groups')
    if (res.data?.code === 1 && res.data.data?.groups) {
      const cloud = res.data.data.groups as RuleGroup[]
      const cloudMap = new Map<string, RuleGroup>()
      cloud.forEach(g => { if (g?.key) cloudMap.set(g.key, g) })
      const keys = new Set<string>()
      const merged: RuleGroup[] = []
      cloud.forEach(g => { if (g?.key && !keys.has(g.key)) { keys.add(g.key); merged.push(g) } })
      ruleGroups.value.forEach(g => { if (g?.key && !keys.has(g.key)) { keys.add(g.key); merged.push(g) } })
      if (!merged.find(g => g.key === '*')) merged.unshift(defaultGroupObj('*'))
      ruleGroups.value = merged
      try { localStorage.setItem(GROUPS_KEY, JSON.stringify(ruleGroups.value)) } catch {}
    }
  } catch {}
}

function matchGroupForUrl(url: string) {
  try {
    const h = new URL(url).hostname.toLowerCase()
    let best: RuleGroup | null = null
    for (const g of ruleGroups.value) {
      if (g.key === '*') continue
      const k = g.key.toLowerCase()
      if (h === k || h.endsWith('.' + k)) {
        if (!best || k.length > best.key.length) best = g
      }
    }
    return best || ruleGroups.value.find(g => g.key === '*') || defaultGroupObj('*')
  } catch {
    return ruleGroups.value.find(g => g.key === '*') || defaultGroupObj('*')
  }
}

function handleGroupSwitch() {
  const g = ruleGroups.value.find(g => g.key === activeGroupKey.value)
  if (g) {
    if (g.defaultLevel) contentLevel.value = g.defaultLevel
    if (g.defaultTags) selectedTagsText.value = g.defaultTags
  }
}

function activateGroupForUrl(url: string) {
  const g = matchGroupForUrl(url)
  activeGroupKey.value = g.key
  handleGroupSwitch()
}

function bindCurrentDomain() {
  const url = form.value.url
  if (!url) { ElMessage.warning('请先输入网页链接'); return }
  try {
    const h = new URL(url).hostname.toLowerCase()
    const src = currentGroup.value
    let g = ruleGroups.value.find(g => g.key === h)
    if (!g) { g = { ...defaultGroupObj(h) }; ruleGroups.value.push(g) }
    Object.assign(g, { kw: src.kw, ext: src.ext, mb: src.mb, must: src.must, defaultLevel: src.defaultLevel, defaultTags: src.defaultTags, tagRegex: src.tagRegex, cssSelector: src.cssSelector })
    activeGroupKey.value = h
    saveGroups()
    flashRulesHint('已保存到规则组「' + h + '」')
  } catch { ElMessage.error('URL 格式不正确') }
}

function createNewGroup() {
  const input = prompt('输入该规则组匹配的域名（如 baidu.com）', '')
  if (input === null) return
  const key = input.trim().replace(/^https?:\/\//, '').replace(/^www\./, '').split('/')[0].split(/[:?#]/)[0].toLowerCase() || '*'
  const src = currentGroup.value
  let g = ruleGroups.value.find(g => g.key === key)
  if (!g) { g = { ...defaultGroupObj(key) }; ruleGroups.value.push(g) }
  Object.assign(g, { kw: src.kw, ext: src.ext, mb: src.mb, must: src.must, defaultLevel: src.defaultLevel, defaultTags: src.defaultTags, tagRegex: src.tagRegex, cssSelector: src.cssSelector })
  activeGroupKey.value = key
  saveGroups()
  flashRulesHint('已创建规则组「' + (key === '*' ? '默认' : key) + '」')
}

function flashRulesHint(msg: string) {
  rulesHintText.value = msg
  setTimeout(() => { rulesHintText.value = '' }, 1600)
}

// ===== Site Cookie =====
const SITE_CK_KEY = 'tgdrive_siteCookies'
const siteCookie = ref('')
const cookieHint = ref('')

function ckStore(): Record<string, string> {
  try { const s = JSON.parse(localStorage.getItem(SITE_CK_KEY) || '{}'); return s && typeof s === 'object' ? s : {} } catch { return {} }
}

function hostOf(url: string) {
  try { return new URL(url).hostname.toLowerCase() } catch { return '' }
}

function syncCookieField(host?: string) {
  if (!host) host = hostOf(form.value.url)
  const store = ckStore()
  siteCookie.value = (host && store[host]) ? store[host] : ''
  cookieHint.value = (host && store[host]) ? '已保存 · ' + host : ''
}

function clearSiteCookie() {
  const host = hostOf(form.value.url)
  if (!host) return
  const store = ckStore()
  delete store[host]
  try { localStorage.setItem(SITE_CK_KEY, JSON.stringify(store)) } catch {}
  siteCookie.value = ''
  cookieHint.value = ''
}

watch(() => form.url, (v) => {
  if (v) syncCookieField()
})

// ===== Tag Library =====
const tagLib = ref<any[]>([])
const pickedTagNames = ref<string[]>([])
const selectedTagsText = ref('')
const showTagPicker = ref(false)
const tagSearchKw = ref('')

const filteredTagLib = computed(() => {
  const kw = tagSearchKw.value.trim().toLowerCase()
  if (!kw) return tagLib.value
  return tagLib.value.filter(t => t.name.toLowerCase().includes(kw))
})

async function fetchTagLib() {
  try {
    const res = await request.get('/tags/public')
    if (res.data?.code === 1) tagLib.value = res.data.data || []
  } catch {}
}

function togglePickTag(name: string) {
  const i = pickedTagNames.value.indexOf(name)
  if (i >= 0) pickedTagNames.value.splice(i, 1)
  else pickedTagNames.value.push(name)
}

function confirmTagPick() {
  selectedTagsText.value = pickedTagNames.value.join(', ')
  showTagPicker.value = false
}

// ===== Parse =====
function extractTagsFromTitle(title: string) {
  const pattern = currentGroup.value.tagRegex?.trim()
  if (!pattern || !title) return
  try {
    const re = new RegExp(pattern, 'g')
    const matches: string[] = []
    let m: RegExpExecArray | null
    while ((m = re.exec(title))) { if (m[1] && !matches.includes(m[1])) matches.push(m[1]) }
    if (!matches.length) return
    const exist = selectedTagsText.value ? selectedTagsText.value.split(/[,，]/).map(s => s.trim()).filter(Boolean) : []
    const merged = [...exist, ...matches]
    const seen = new Set<string>()
    selectedTagsText.value = merged.filter(t => { const k = t.toLowerCase(); if (seen.has(k)) return false; seen.add(k); return true }).join(', ')
  } catch {}
}

function isDirectImg(u: string) { return /\.(?:jpg|jpeg|png|gif|webp|avif|bmp|ico|svg|tif|tiff)(?:\?|#|$)/i.test(u) }

function fragmentImgUrls(txt: string): string[] {
  const out: string[] = []
  const re = /\b(?:data-src|data-original|data-lazy-src|src)\s*=\s*["']([^"']+)["']/gi
  let m: RegExpExecArray | null
  while ((m = re.exec(txt))) {
    const v = m[1].trim()
    if (/^https?:\/\//i.test(v)) out.push(v)
  }
  return out
}

function distinctHttp(str: string): string[] {
  const out: string[] = []
  const re = /\bhttps?:\/\/[^\s\u4e00-\u9fff"'<>()]+/gi
  let m: RegExpExecArray | null
  while ((m = re.exec(str || ''))) {
    const t = m[0].trim().replace(/[.,;:!?、。，；：！？…）】\u2026]+$/g, '')
    if (t && !out.includes(t)) out.push(t)
  }
  return out
}

function imgKey(u: string) { return u.replace(/^https?:\/\//i, 'https://').replace(/^(https:\/\/[^/]*)/i, s => s.toLowerCase()) }
function dedupCands(list: string[]): string[] {
  const idx: Record<string, number> = {}
  const out: string[] = []
  for (const u of list) {
    const k = imgKey(u)
    if (idx[k] !== undefined) {
      if (/^https:\/\//i.test(u) && /^http:\/\//i.test(out[idx[k]])) out[idx[k]] = u
      continue
    }
    idx[k] = out.length
    out.push(u)
  }
  return out
}

function localFilterUrls(list: string[]): string[] {
  const r = currentGroup.value
  const kws = (r.kw || '').split(/[,，;；]/).map(s => s.trim().toLowerCase()).filter(Boolean)
  const exts = (r.ext || '').split(/[,，;；]/).map(s => s.trim().toLowerCase().replace(/^\./, '')).filter(Boolean)
  const musts = (r.must || '').split(/[,，;；]/).map(s => s.trim().toLowerCase()).filter(Boolean)
  return list.filter(u => {
    const low = u.toLowerCase()
    for (const kw of kws) { if (low.includes(kw)) return false }
    const m = low.match(/\.([a-z0-9]{1,8})(?:\?.*)?$/)
    const e = m ? m[1] : ''
    if (e && exts.includes(e)) return false
    if (musts.length && !musts.some(w => low.includes(w))) return false
    return true
  })
}

function connectParseWs(pageUrl: string) {
  if (parseWs) {
    parseWs.close()
    parseWs = null
  }
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const token = localStorage.getItem('token') || ''
  const wsUrl = `${protocol}//${location.host}/ws/parse-progress?token=${encodeURIComponent(token)}`
  parseWs = new WebSocket(wsUrl)
  parseWs.onopen = () => {
    // 订阅该 pageUrl 的进度
    parseWs?.send(JSON.stringify({ pageUrl }))
  }
  parseWs.onmessage = (ev) => {
    try {
      const data = JSON.parse(ev.data)
      if (data.pageUrl === pageUrl && data.message) {
        parseProgress.value = data.message
        if (data.status === 'progress') {
          statusText.value = data.message
          floatLogs.value.push(data.message)
          if (floatLogs.value.length > 20) floatLogs.value.shift()
        }
      }
    } catch (e) {}
  }
  parseWs.onerror = () => {}
  parseWs.onclose = () => { parseWs = null }
}

async function handleParse() {
  if (!form.value.url) { ElMessage.warning('请输入网页地址'); return }

  parsing.value = true
  parsed.value = false
  cands.value = []
  selectedCandIndices.value = []
  parseProgress.value = ''

  const rawText = form.value.url.trim()

  // Detect pasted img fragments
  const pulled = /(<img\b|data-src\s*=|src\s*=\s*")/i.test(rawText) ? fragmentImgUrls(rawText) : []
  if (pulled.length) {
    cands.value = dedupCands(localFilterUrls(pulled))
    parsed.value = true
    selectedCandIndices.value = cands.value.map((_, i) => i)
    buildXhsMap()
    statusText.value = `直链识别成功：共 ${cands.value.length} 个候选`
    statusClass.value = 'ok'
    parsing.value = false
    showFloatPanel.value = true
    floatPanelStatus.value = 'done'
    floatLogs.value = [`识别到 ${cands.value.length} 个直链`]
    return
  }

  // Detect multiple direct image URLs
  const urlAll = distinctHttp(rawText)
  const imgOnes = urlAll.filter(isDirectImg)
  if (urlAll.length > 1 && imgOnes.length === urlAll.length) {
    cands.value = dedupCands(localFilterUrls(urlAll))
    parsed.value = true
    selectedCandIndices.value = cands.value.map((_, i) => i)
    buildXhsMap()
    statusText.value = `直链识别成功：共 ${cands.value.length} 个候选`
    statusClass.value = 'ok'
    parsing.value = false
    showFloatPanel.value = true
    floatPanelStatus.value = 'done'
    floatLogs.value = [`识别到 ${cands.value.length} 个直链`]
    return
  }

  // Single URL - parse page
  const target = urlAll.length === 1 ? urlAll[0] : ''
  if (!target) { statusText.value = '未识别到 http(s) 链接'; statusClass.value = 'err'; parsing.value = false; return }

  activateGroupForUrl(target)
  syncCookieField()

  try {
    const payload: any = { url: target }
    if (siteCookie.value) payload.cookie = siteCookie.value
    if (form.value.browserMode) {
      payload.browserMode = 'true'
      payload.deviceMode = form.value.deviceMode
    }
    if (currentGroup.value.cssSelector) {
      payload.cssSelector = currentGroup.value.cssSelector
    }

    const deviceLabel = form.value.deviceMode === 'h5' ? 'H5手机模式' : 'PC桌面模式'
    statusText.value = form.value.browserMode ? `正在使用${deviceLabel}解析...` : '解析中...'
    statusClass.value = 'loading'

    // 显示浮窗
    showFloatPanel.value = true
    floatPanelStatus.value = 'parsing'
    floatLogs.value = []

    // 浏览器模式：连接WebSocket接收实时进度
    if (form.value.browserMode) {
      connectParseWs(target)
    }

    // 浏览器模式使用更长超时（120秒）
    const client = form.value.browserMode ? longTimeoutService : request
    const response = await client.post('/import/parse-page', payload)
    if (response.data.code === 1) {
      const result = response.data.data
      pageTitle.value = result.title || ''
      extractTagsFromTitle(pageTitle.value)
      const rawImages = (result.images || []).map((img: any) => img.url || img)
      cands.value = dedupCands(localFilterUrls(rawImages))
      parsed.value = true
      selectedCandIndices.value = cands.value.map((_, i) => i)
      buildXhsMap()
      const rawN = rawImages.length
      const filteredN = rawN - cands.value.length
      statusText.value = filteredN > 0
        ? `解析成功：找到 ${cands.value.length} 个候选（提取 ${rawN} 个，忽略规则过滤 ${filteredN} 个）`
        : `解析成功：找到 ${cands.value.length} 个候选`
      statusClass.value = 'ok'
      floatPanelStatus.value = 'done'
      floatLogs.value.push(`找到 ${cands.value.length} 个资源`)
    } else {
      statusText.value = response.data.message || '解析失败'
      statusClass.value = 'err'
      floatPanelStatus.value = 'error'
      floatLogs.value.push(response.data.message || '解析失败')
    }
  } catch (error: any) {
    statusText.value = '解析失败: ' + (error.response?.data?.message || error.message)
    statusClass.value = 'err'
    floatPanelStatus.value = 'error'
    floatLogs.value.push(error.message)
  } finally {
    parsing.value = false
    parseProgress.value = ''
    if (parseWs) { parseWs.close(); parseWs = null }
  }
}

// ===== Selection =====
const selectedCands = computed(() => selectedCandIndices.value.map(i => {
  const raw = cands.value[i]
  return xhsCandMap.value.get(raw) || raw
}))

function toggleCand(index: number) {
  const i = selectedCandIndices.value.indexOf(index)
  if (i >= 0) selectedCandIndices.value.splice(i, 1)
  else selectedCandIndices.value.push(index)
}

function selectAllCands() { selectedCandIndices.value = cands.value.map((_, i) => i) }
function clearSelCands() { selectedCandIndices.value = [] }
function selectFirst20() { selectedCandIndices.value = cands.value.slice(0, 20).map((_, i) => i) }

async function copySelectedUrls() {
  const urls = selectedCands.value.join('\n')
  try {
    await navigator.clipboard.writeText(urls)
    ElMessage.success('已复制 ' + selectedCands.value.length + ' 个地址')
  } catch { ElMessage.error('复制失败') }
}

async function copyAllXhsUrls() {
  const xhsUrls = cands.value
    .filter(url => isXhsDomain(url))
    .map(url => xhsCdnToCiUrl(url))
  const unique = [...new Set(xhsUrls)]
  if (unique.length === 0) { ElMessage.warning('没有小红书图片'); return }
  try {
    await navigator.clipboard.writeText(unique.join('\n'))
    ElMessage.success('已复制 ' + unique.length + ' 个 ci.xiaohongshu.com 地址')
  } catch { ElMessage.error('复制失败') }
}

// ===== XHS CDN Processing =====
const xhsCandMap = ref<Map<string, string>>(new Map())
const showXhsExtractBtn = computed(() => {
  return cands.value.some(url => isXhsDomain(url))
})

function isXhsDomain(url: string): boolean {
  return /xhscdn\.com|ci\.xiaohongshu\.com/i.test(url)
}

function xhsCdnToCiUrl(url: string): string {
  // Match any xhscdn subdomain, capture path after timestamp+md5, before !
  const reg = /^https?:\/\/[^.]+\.xhscdn\.com\/\d+\/[0-9a-f]+\/(.+?)!/;
  const match = url.match(reg);
  if (match && match[1]) {
    return `https://ci.xiaohongshu.com/${match[1]}`;
  }
  // No ! suffix scenario
  const regNoSuffix = /^https?:\/\/[^.]+\.xhscdn\.com\/\d+\/[0-9a-f]+\/(.+)$/;
  const match2 = url.match(regNoSuffix);
  if (match2 && match2[1]) {
    return `https://ci.xiaohongshu.com/${match2[1]}`;
  }
  // ci.xiaohongshu.com already - return as-is
  if (/ci\.xiaohongshu\.com/i.test(url)) {
    return url;
  }
  return url;
}

function buildXhsMap() {
  const map = new Map<string, string>()
  for (const url of cands.value) {
    if (isXhsDomain(url)) {
      const converted = xhsCdnToCiUrl(url)
      if (converted !== url) {
        map.set(url, converted)
      }
    }
  }
  xhsCandMap.value = map
}

function getCandDisplayUrl(url: string): string {
  return xhsCandMap.value.get(url) || url
}

function extractXhsImages() {
  // Re-filter cands to only keep XHS images, using converted URLs
  const xhsUrls: string[] = []
  for (const url of cands.value) {
    if (isXhsDomain(url)) {
      const converted = xhsCdnToCiUrl(url)
      if (!xhsUrls.includes(converted)) {
        xhsUrls.push(converted)
      }
    }
  }
  if (xhsUrls.length === 0) {
    ElMessage.warning('未找到小红书图片')
    return
  }
  // Replace cands with converted XHS URLs
  cands.value = xhsUrls
  xhsCandMap.value = new Map()
  selectedCandIndices.value = cands.value.map((_, i) => i)
  statusText.value = `已提取 ${xhsUrls.length} 张小红书图片（ci.xiaohongshu.com）`
  statusClass.value = 'ok'
}

// ===== Paste =====
async function handlePaste() {
  try {
    const text = await navigator.clipboard.readText()
    if (text) form.value.url = text
  } catch { ElMessage.error('无法读取剪贴板') }
}

// ===== Batch Import =====
async function handleBatchImport() {
  if (selectedCands.value.length === 0) return

  importing.value = true
  paused.value = false
  importResult.value = null
  pendingUrls = [...selectedCands.value]
  importProgress.value = { total: pendingUrls.length, completed: 0, failed: 0, currentUrl: '' }

  // Connect WebSocket
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(`${protocol}//${window.location.host}/ws/import-progress`)
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'import_progress') {
        importProgress.value.completed = data.completed
        importProgress.value.failed = data.failed
        if (data.filename) importProgress.value.currentUrl = data.filename
      } else if (data.type === 'import_complete') {
        importResult.value = { completed: data.completed, failed: data.failed }
        importing.value = false
        ElMessage.success(`导入完成: 成功 ${data.completed} 个，失败 ${data.failed} 个`)
        closeWs()
      }
    } catch {}
  }
  ws.onerror = () => { ElMessage.warning('WebSocket连接失败，将继续导入但无法获取实时进度') }

  await processNextBatch()
}

async function processNextBatch() {
  if (paused.value || pendingUrls.length === 0) {
    if (pendingUrls.length === 0 && importing.value) {
      importResult.value = {
        completed: importProgress.value.completed,
        failed: importProgress.value.failed
      }
      importing.value = false
      closeWs()
    }
    return
  }

  const batch = pendingUrls.splice(0, concurrency.value)
  abortController = new AbortController()

  try {
    const res = await request.post('/import/url-with-source', {
      urls: batch,
      sourcePage: form.value.url || 'web-scrape',
      tags: selectedTagsText.value ? selectedTagsText.value.split(/[,，]/).map(s => s.trim()).filter(Boolean) : undefined,
      contentLevel: contentLevel.value,
      cookie: siteCookie.value || undefined
    })

    if (res.data?.code === 1) {
      const result = res.data.data || {}
      importProgress.value.completed += (result.completed || 0)
      importProgress.value.failed += (result.failed || 0)
    }
  } catch (e: any) {
    importProgress.value.failed += batch.length
  }

  if (!paused.value) await processNextBatch()
}

function togglePause() {
  paused.value = !paused.value
  if (!paused.value) processNextBatch()
}

function closeWs() {
  if (ws) { try { ws.close() } catch {} ws = null }
}

onBeforeUnmount(() => { closeWs(); if (abortController) abortController.abort() })

// ===== Helpers =====
function truncName(url: string) {
  const name = url.replace(/^https?:\/\//, '').slice(0, 60)
  return name
}

function handleImageError(event: Event) {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

function isImageUrl(url: string): boolean {
  return /\.(jpg|jpeg|png|gif|webp|avif|bmp|ico|svg|tif|tiff)(\?|#|$)/i.test(url)
}

function isVideoUrl(url: string): boolean {
  return /\.(mp4|webm|ogg|mov|avi|mkv|m3u8)(\?|#|$)/i.test(url)
}

function openPreview(url: string) {
  window.open(url, '_blank')
}

function handleVideoLoad(event: Event, index: number) {
  const video = event.target as HTMLVideoElement
  video.currentTime = 1
}

// ===== Init =====
onMounted(async () => {
  loadGroups()
  await Promise.all([fetchTagLib(), pullGroupsFromCloud()])
  syncCookieField()
})
</script>

<style scoped>
.web-scrape-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.scrape-card, .cand-card { margin-bottom: 16px; }

.input-group {
  display: flex;
  gap: 10px;
}

.input-group .el-input { flex: 1; }

.status { margin-top: 10px; font-size: 13px; min-height: 18px; }
.status.ok { color: #67c23a; }
.status.err { color: #f56c6c; }

.form-tip { font-size: 12px; color: #909399; margin-top: 4px; }

.opts-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.opts-row label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.ign-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
  align-items: center;
}

.ig-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  flex: 0 0 70px;
}

.group-select { flex: 0 0 200px; }

.rules-hint {
  font-size: 11px;
  color: #67c23a;
  white-space: nowrap;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.count { font-size: 12px; color: #909399; }
.count b { color: #fff; }

.progress-wrap { margin: 12px 0; }
.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.prog-url { flex: 1; color: #409eff; word-break: break-all; }

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  margin-top: 14px;
  max-height: 600px;
  overflow-y: auto;
}

.image-item {
  position: relative;
  background: #141620;
  border: 2px solid #303050;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.15s;
}

.image-item:hover { border-color: #409eff; }
.image-item.selected { border-color: #409eff; }

.image-checkbox {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 16px;
  height: 16px;
  z-index: 2;
  accent-color: #409eff;
}

.image-preview {
  width: 100%;
  height: 110px;
  background: #1a1c30;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-name {
  font-size: 10px;
  color: #909399;
  padding: 4px 6px;
  word-break: break-all;
  height: 28px;
  line-height: 13px;
  overflow: hidden;
}

.no-images { padding: 40px 0; }

.import-result { margin-top: 16px; }

.tag-picker-body {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
  padding: 8px 0;
}

.tchip {
  cursor: pointer;
  user-select: none;
  padding: 6px 12px;
  border-radius: 18px;
  border: 1px solid #303050;
  background: #141620;
  color: #c0c4cc;
  font-size: 13px;
  transition: all 0.15s;
}

.tchip:hover { border-color: #409eff; }
.tchip.on {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.15);
  color: #fff;
}

/* Floating Panel */
.float-panel {
  position: fixed;
  z-index: 9999;
  width: 320px;
  background: #1a1c30;
  border: 1px solid #303050;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
  overflow: hidden;
  user-select: none;
}

.float-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #141620;
  border-bottom: 1px solid #303050;
  cursor: move;
}

.float-title {
  font-size: 13px;
  font-weight: 600;
  color: #e0e0e0;
}

.float-close {
  color: #909399;
  font-size: 14px;
}

.float-body {
  padding: 12px 14px;
  max-height: 300px;
  overflow-y: auto;
}

.float-spinner {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #c0c4cc;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #303050;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.float-result {
  font-size: 13px;
  font-weight: 500;
}

.float-log {
  margin-top: 10px;
  max-height: 160px;
  overflow-y: auto;
}

.float-log-item {
  font-size: 11px;
  color: #909399;
  padding: 2px 0;
  border-bottom: 1px solid #202040;
  word-break: break-all;
}

/* Image/Video Preview */
.preview-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 11px;
  text-align: center;
  padding: 4px 0;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.image-item:hover .preview-hint {
  opacity: 1;
}

.video-thumb {
  position: relative;
  width: 100%;
  height: 100%;
}

.video-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 36px;
  height: 36px;
  background: rgba(0,0,0,0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  pointer-events: none;
}

/* XHS Badge */
.xhs-badge {
  display: inline-block;
  font-size: 9px;
  font-weight: 700;
  color: #ff2442;
  background: rgba(255,36,66,0.12);
  border: 1px solid rgba(255,36,66,0.3);
  border-radius: 3px;
  padding: 0 4px;
  margin-right: 4px;
  vertical-align: middle;
  line-height: 15px;
}

.xhs-converted {
  color: #67c23a;
  font-size: 10px;
}

.xhs-original {
  display: block;
  color: #606266;
  font-size: 9px;
  text-decoration: line-through;
  margin-top: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
