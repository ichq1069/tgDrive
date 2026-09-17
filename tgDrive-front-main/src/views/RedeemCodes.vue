<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Ticket /></el-icon>
            <span>兑换码管理</span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="showCreateDialog = true">生成兑换码</el-button>
            <el-button type="default" @click="fetchList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="codeList" v-loading="loading" height="calc(100vh - 280px)" style="width: 100%;">
        <el-table-column prop="code" label="兑换码" width="180" />
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.type === 'once' ? 'danger' : 'success'" size="small">
              {{ scope.row.type === 'once' ? '一次性' : '可复用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetLevel" label="目标等级" width="100" align="center" />
        <el-table-column prop="maxUses" label="最大使用次数" width="130" align="center" />
        <el-table-column prop="usedCount" label="已使用次数" width="110" align="center" />
        <el-table-column prop="expireTime" label="过期时间" width="180" align="center">
          <template #default="scope">
            {{ scope.row.expireTime ? formatTime(scope.row.expireTime) : '永不过期' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" @click="deleteCode(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="生成兑换码" width="400px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="类型">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="一次性 (全站一次)" value="once" />
            <el-option label="可复用 (每用户一次)" value="repeatable" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标等级">
          <el-select v-model="createForm.targetLevel" style="width: 100%">
            <el-option label="VIP" value="vip" />
            <el-option label="SVIP" value="svip" />
            <el-option label="VVIP" value="vvip" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="createForm.expireTime" type="datetime" placeholder="留空永不过期" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createCode" :loading="submitLoading">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ticket } from '@element-plus/icons-vue'
import request from '@/utils/request'

const codeList = ref<any[]>([])
const loading = ref(false)
const showCreateDialog = ref(false)
const submitLoading = ref(false)
const createForm = ref({ type: 'once', targetLevel: 'vip', expireTime: null as Date | null })

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/redeem-codes')
    if (res.data?.code === 1) {
      codeList.value = res.data.data || []
    }
  } catch (e) {
    ElMessage.error('获取兑换码列表失败')
  } finally {
    loading.value = false
  }
}

const createCode = async () => {
  submitLoading.value = true
  try {
    const payload: any = { type: createForm.value.type, targetLevel: createForm.value.targetLevel }
    if (createForm.value.expireTime) {
      payload.expireTime = Math.floor(createForm.value.expireTime.getTime() / 1000)
    }
    const res = await request.post('/admin/redeem-codes', payload)
    if (res.data?.code === 1) {
      ElMessage.success('兑换码已生成')
      showCreateDialog.value = false
      createForm.value = { type: 'once', targetLevel: 'vip', expireTime: null }
      fetchList()
    } else {
      ElMessage.error(res.data?.msg || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成失败')
  } finally {
    submitLoading.value = false
  }
}

const deleteCode = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该兑换码？', '确认删除', { type: 'warning' })
    const res = await request.delete(`/admin/redeem-codes/${id}`)
    if (res.data?.code === 1) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const formatTime = (ts: number) => new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false })

onMounted(() => fetchList())
</script>
