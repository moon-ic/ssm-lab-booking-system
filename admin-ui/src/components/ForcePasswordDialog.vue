<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少 8 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true

  try {
    await authStore.updatePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    ElMessage.success('密码修改成功，可以继续使用系统')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <ElDialog
    :model-value="Boolean(authStore.state.session?.firstLoginRequired)"
    width="460px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <template #header>
      <div class="dialog-title">首次登录，请先修改密码</div>
    </template>

    <p class="dialog-tip">
      系统检测到当前账号仍在使用初始密码。根据业务规则，修改成功后才能继续处理后续模块。
    </p>

    <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
      <ElFormItem label="当前密码" prop="oldPassword">
        <ElInput v-model="form.oldPassword" type="password" show-password />
      </ElFormItem>
      <ElFormItem label="新密码" prop="newPassword">
        <ElInput v-model="form.newPassword" type="password" show-password />
      </ElFormItem>
      <ElFormItem label="确认新密码" prop="confirmPassword">
        <ElInput v-model="form.confirmPassword" type="password" show-password />
      </ElFormItem>
    </ElForm>

    <template #footer>
      <ElButton type="primary" :loading="loading" @click="submit">
        确认修改
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.dialog-tip {
  margin: 0 0 18px;
  line-height: 1.7;
  color: #475569;
}
</style>
