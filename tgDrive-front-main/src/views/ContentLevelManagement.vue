<template>
  <div class="content-level-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>权限级别管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新增级别
          </el-button>
        </div>
      </template>

      <el-table :data="levels" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="级别名称" width="150">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.name)">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="levelOrder" label="排序" width="100" align="center" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此级别？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑级别' : '新增级别'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="级别名称" required>
          <el-input v-model="form.name" placeholder="如: pt, vip, svip, vvip" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.levelOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ContentLevel {
  id: number
  name: string
  levelOrder: number
  description: string
}

const levels = ref<ContentLevel[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)

const form = ref({
  name: '',
  levelOrder: 0,
  description: ''
})

const getLevelType = (name: string) => {
  const map: Record<string, string> = {
    vvip: 'danger',
    svip: 'warning',
    vip: 'success',
    pt: 'info'
  }
  return map[name] || 'info'
}

const fetchLevels = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/content-levels')
    if (res.data.code === 1) {
      levels.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', levelOrder: levels.value.length, description: '' }
  dialogVisible.value = true
}

const showEditDialog = (row: ContentLevel) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { name: row.name, levelOrder: row.levelOrder, description: row.description || '' }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入级别名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await request.patch(`/admin/content-levels/${editId.value}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/admin/content-levels', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchLevels()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await request.delete(`/admin/content-levels/${id}`)
    ElMessage.success('删除成功')
    fetchLevels()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchLevels()
})
</script>

<style scoped>
.content-level-management {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
