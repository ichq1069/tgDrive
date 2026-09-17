<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Lock /></el-icon>
            <span>私密库白名单</span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="showAddDialog = true">添加用户</el-button>
            <el-button type="default" @click="fetchList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="whitelist" v-loading="loading" height="calc(100vh - 280px)" style="width: 100%;">
        <el-table-column prop="userId" label="用户 ID" width="100" align="center" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="addTime" label="加入时间" width="180" align="center">
          <template #default="scope">{{ formatTime(scope.row.addTime) }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" @click="removeUser(scope.row.userId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showAddDialog" title="添加白名单用户" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="addUsername" placeholder="请输入用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addUser" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

const whitelist = ref<any[]>([])
const loading = ref(false)
const showAddDialog = ref(false)
const submitLoading = ref(false)
const addUsername = ref('')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/private-whitelist')
    if (res.data?.code === 1) {
      whitelist.value = res.data.data || []
    }
  } catch (e) {
    ElMessage.error('获取白名单失败')
  } finally {
    loading.value = false
  }
}

const addUser = async () => {
  if (!addUsername.value.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  submitLoading.value = true
  try {
    const res = await request.post('/admin/private-whitelist', { username: addUsername.value.trim() })
    if (res.data?.code === 1) {
      ElMessage.success('添加成功')
      showAddDialog.value = false
      addUsername.value = ''
      fetchList()
    } else {
      ElMessage.error(res.data?.msg || '添加失败')
    }
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    submitLoading.value = false
  }
}

const removeUser = async (userId: number) => {
  try {
    await ElMessageBox.confirm('确定将该用户从白名单移除？', '确认移除', { type: 'warning' })
    const res = await request.delete(`/admin/private-whitelist/${userId}`)
    if (res.data?.code === 1) {
      ElMessage.success('移除成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('移除失败')
  }
}

const formatTime = (ts: number) => new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false })

onMounted(() => fetchList())
</script>
