<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { importDevice } from '@/api/device-imports'
import { useAuthStore } from '@/store/auth'
import type { DeviceItem } from '@/types/device'

const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()
const importResult = ref<DeviceItem | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const canImport = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN')

const form = reactive({
  deviceName: '',
  category: '',
  location: '',
  description: ''
})

const rules = {
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

function revokePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  selectedFile.value = file
  revokePreview()

  if (file) {
    previewUrl.value = URL.createObjectURL(file)
  }
}

function resetForm() {
  form.deviceName = ''
  form.category = ''
  form.location = ''
  form.description = ''
  selectedFile.value = null
  importResult.value = null
  revokePreview()
}

async function submitImport() {
  await formRef.value?.validate()

  if (!selectedFile.value) {
    ElMessage.error('请选择图片文件')
    return
  }

  loading.value = true

  try {
    importResult.value = await importDevice({
      deviceName: form.deviceName,
      category: form.category,
      location: form.location,
      description: form.description,
      image: selectedFile.value
    })
    ElMessage.success('设备导入成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  revokePreview()
})
</script>

<template>
  <div class="import-page">
    <section class="hero-card">
      <span class="eyebrow">设备导入</span>
      <h2>上传图片并登记新设备</h2>
      <p>
        该模块遵循后端导入流程：设备名称必填，其他信息可选，且必须上传一张图片。导入成功后会返回生成的设备 ID
        和设备编号。
      </p>
    </section>

    <section v-if="canImport" class="workspace-grid">
      <article class="form-card">
        <div class="card-header">
          <div>
            <h3>导入表单</h3>
            <p>填写设备基础信息并上传一张图片。</p>
          </div>
        </div>

        <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
          <ElFormItem label="设备名称" prop="deviceName">
            <ElInput v-model="form.deviceName" placeholder="请输入设备名称" />
          </ElFormItem>
          <ElFormItem label="分类">
            <ElInput v-model="form.category" placeholder="选填分类" />
          </ElFormItem>
          <ElFormItem label="位置">
            <ElInput v-model="form.location" placeholder="选填位置" />
          </ElFormItem>
          <ElFormItem label="描述">
            <ElInput v-model="form.description" type="textarea" :rows="4" placeholder="选填描述" />
          </ElFormItem>
          <ElFormItem label="设备图片">
            <input class="file-input" type="file" accept="image/*" @change="handleFileChange" />
          </ElFormItem>
        </ElForm>

        <div class="form-actions">
          <ElButton @click="resetForm">重置</ElButton>
          <ElButton type="primary" :loading="loading" @click="submitImport">导入设备</ElButton>
        </div>
      </article>

      <article class="preview-card">
        <div class="card-header">
          <div>
            <h3>预览</h3>
            <p>导入前可先查看图片和生成结果面板。</p>
          </div>
        </div>

        <div class="preview-panel">
          <img v-if="previewUrl" :src="previewUrl" alt="设备预览" class="preview-image" />
          <div v-else class="preview-empty">暂未选择图片</div>
        </div>

        <div v-if="importResult" class="result-card">
          <strong>{{ importResult.deviceName }}</strong>
          <span>{{ importResult.deviceCode }}</span>
          <span>{{ importResult.status }} / {{ importResult.location }}</span>
          <p>{{ importResult.description || '暂无描述' }}</p>
        </div>
      </article>
    </section>

    <section v-else class="forbidden-card">
      <h3>需要导入权限</h3>
      <p>只有管理员和超级管理员可以访问设备导入页面，当前角色仍可继续浏览设备库存。</p>
    </section>
  </div>
</template>

<style scoped>
.import-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.form-card,
.preview-card,
.forbidden-card {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.hero-card h2 {
  margin: 10px 0 12px;
  font-size: 30px;
}

.hero-card p,
.forbidden-card p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
}

.card-header {
  margin-bottom: 18px;
}

.card-header h3 {
  margin: 0;
}

.card-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.file-input {
  width: 100%;
  padding: 12px;
  border: 1px dashed #94a3b8;
  border-radius: 14px;
  background: #f8fafc;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.preview-panel {
  display: grid;
  place-items: center;
  min-height: 240px;
  margin-bottom: 18px;
  border-radius: 20px;
  background: #f8fafc;
  overflow: hidden;
}

.preview-image {
  width: 100%;
  height: 240px;
  object-fit: cover;
}

.preview-empty {
  color: #94a3b8;
}

.result-card {
  display: grid;
  gap: 6px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(224, 233, 246, 0.78);
}

.result-card span,
.result-card p {
  margin: 0;
  color: #475569;
}

@media (max-width: 980px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>


