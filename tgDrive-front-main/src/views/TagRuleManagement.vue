<template>
  <div class="tag-rule-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标签规则管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新增规则
          </el-button>
        </div>
      </template>

      <el-table :data="rules" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="规则名称" width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ ruleTypeLabel(row.ruleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ruleValue" label="规则值" show-overflow-tooltip />
        <el-table-column label="关联标签" width="120">
          <template #default="{ row }">
            <el-tag v-if="getTag_name(row.tagId)" type="info">{{ getTag_name(row.tagId) }}</el-tag>
            <span v-else class="text-muted">未知标签</span>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此规则？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.name" placeholder="如: 图片自动标签" />
        </el-form-item>
        <el-form-item label="规则类型" required>
          <el-select v-model="form.ruleType" placeholder="选择类型">
            <el-option label="文件扩展名" value="extension" />
            <el-option label="文件名包含" value="contains" />
            <el-option label="正则匹配" value="regex" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则值" required>
          <el-input v-model="form.ruleValue" :placeholder="ruleValuePlaceholder" />
          <div class="form-tip">{{ ruleValueTip }}</div>
        </el-form-item>
        <el-form-item label="关联标签" required>
          <el-select v-model="form.tagId" placeholder="选择标签" filterable>
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface TagRule {
  id: number
  name: string
  ruleType: string
  ruleValue: string
  tagId: number
  enabled: number
}

interface Tag {
  id: number
  name: string
}

const rules = ref<TagRule[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)

const form = ref({
  name: '',
  ruleType: 'extension',
  ruleValue: '',
  tagId: null as number | null,
  enabled: 1
})

const ruleTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    extension: '扩展名',
    contains: '文件名包含',
    regex: '正则匹配'
  }
  return map[type] || type
}

const ruleValuePlaceholder = computed(() => {
  switch (form.value.ruleType) {
    case 'extension': return '如: .jpg,.png,.gif'
    case 'contains': return '如: photo,screenshot'
    case 'regex': return '如: .*\\.(jpg|png)$'
    default: return ''
  }
})

const ruleValueTip = computed(() => {
  switch (form.value.ruleType) {
    case 'extension': return '多个扩展名用逗号分隔，如 .jpg,.png,.gif'
    case 'contains': return '多个关键词用逗号分隔，文件名包含任一关键词即匹配'
    case 'regex': return '使用正则表达式匹配文件名'
    default: return ''
  }
})

const getTag_name = (tagId: number) => {
  const tag = tags.value.find(t => t.id === tagId)
  return tag ? tag.name : null
}

const fetchRules = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/tag-rules')
    if (res.data.code === 1) {
      rules.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const fetchTags = async () => {
  try {
    const res = await request.get('/admin/tags')
    if (res.data.code === 1) {
      tags.value = res.data.data
    }
  } catch (e) {
    // ignore
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', ruleType: 'extension', ruleValue: '', tagId: null, enabled: 1 }
  dialogVisible.value = true
}

const showEditDialog = (row: TagRule) => {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    name: row.name,
    ruleType: row.ruleType,
    ruleValue: row.ruleValue,
    tagId: row.tagId,
    enabled: row.enabled
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name.trim() || !form.value.ruleValue.trim() || !form.value.tagId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await request.patch(`/admin/tag-rules/${editId.value}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/tag-rules', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchRules()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await request.delete(`/admin/tag-rules/${id}`)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchRules()
  fetchTags()
})
</script>

<style scoped>
.tag-rule-management {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.text-muted {
  color: #909399;
}
</style>
