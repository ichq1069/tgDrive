<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Ticket /></el-icon>
            <span>兑换会员等级</span>
          </div>
        </div>
      </template>

      <div class="redeem-form">
        <el-form :model="redeemForm" label-width="100px" style="max-width: 500px;">
          <el-form-item label="兑换码">
            <el-input v-model="redeemForm.code" placeholder="请输入兑换码" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleRedeem" :loading="loading" :disabled="!redeemForm.code.trim()">立即兑换</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Ticket } from '@element-plus/icons-vue'
import request from '@/utils/request'

const redeemForm = ref({ code: '' })
const loading = ref(false)

const handleRedeem = async () => {
  if (!redeemForm.value.code.trim()) {
    ElMessage.warning('请输入兑换码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/redeem', { code: redeemForm.value.code.trim() })
    if (res.data?.code === 1) {
      ElMessage.success('兑换成功！会员等级已提升')
      redeemForm.value.code = ''
    } else {
      ElMessage.error(res.data?.msg || '兑换失败')
    }
  } catch (e: any) {
    const msg = e?.response?.data?.msg || '兑换失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.redeem-form {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}
</style>
