<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><PriceTag /></el-icon>
            <span>标签管理</span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="showCreateDialog = true">新建标签</el-button>
            <el-button type="default" @click="fetchTags">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tagList" v-loading="loading" height="calc(100vh - 280px)" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="标签名" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileCount" label="关联文件数" width="120" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" @click="deleteTag(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="新建标签" width="400px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="标签名">
          <el-input v-model="createForm.name" placeholder="请输入标签名" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createTag" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PriceTag } from '@element-plus/icons-vue'
import request from '@/utils/request'

const tagList = ref<any[]>([])
const loading = ref(false)
const showCreateDialog = ref(false)
const submitLoading = ref(false)
const createForm = ref({ name: '', description: '' })

const fetchTags = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/tags')
    if (res.data?.code === 1) {
      tagList.value = res.data.data || []
    }
  } catch (e) {
    ElMessage.error('获取标签列表失败')
  } finally {
    loading.value = false
  }
}

const createTag = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('标签名不能为空')
    return
  }
  submitLoading.value = true
  try {
    const res = await request.post('/admin/tags', createForm.value)
    if (res.data?.code === 1) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      createForm.value = { name: '', description: '' }
      fetchTags()
    } else {
      ElMessage.error(res.data?.msg || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    submitLoading.value = false
  }
}

const deleteTag = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该标签？关联标签的文件不会被删除。', '确认删除', { type: 'warning' })
    const res = await request.delete(`/admin/tags/${id}`)
    if (res.data?.code === 1) {
      ElMessage.success('删除成功')
      fetchTags()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const formatTime = (ts: number) => new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false })

onMounted(() => fetchTags())
</script>
