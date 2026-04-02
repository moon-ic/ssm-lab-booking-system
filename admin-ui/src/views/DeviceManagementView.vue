<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createDevice,
  deviceStatusOptions,
  getDeviceDetail,
  listDevices,
  updateDevice,
  updateDeviceStatus
} from '@/api/devices'
import { useAuthStore } from '@/store/auth'
import type {
  DeviceItem,
  DeviceListQuery,
  DeviceStatus,
  SaveDevicePayload
} from '@/types/device'

const authStore = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const saveDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const saveFormRef = ref()

const filters = reactive<DeviceListQuery>({
  keyword: '',
  category: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const page = reactive({
  list: [] as DeviceItem[],
  total: 0
})

const detail = ref<DeviceItem | null>(null)

const saveForm = reactive({
  deviceId: undefined as number | undefined,
  deviceName: '',
  deviceCode: '',
  category: '',
  location: '',
  imageUrl: '',
  description: ''
})

const saveRules = {
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  location: [{ required: true, message: '请输入存放地点', trigger: 'blur' }]
}

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const canManage = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN')
const categories = computed(() => [...new Set(page.list.map((item) => item.category))])

function deviceStatusLabel(status: DeviceStatus) {
  switch (status) {
    case 'AVAILABLE':
      return '可用'
    case 'DISABLED':
      return '停用'
    case 'BORROWED':
      return '借出中'
    case 'REPAIRING':
      return '维修中'
    case 'DAMAGED':
      return '已损坏'
    default:
      return status
  }
}

async function loadDevices() {
  loading.value = true

  try {
    const data = await listDevices(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载设备列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadDevices()
}

function handleReset() {
  filters.keyword = ''
  filters.category = undefined
  filters.status = undefined
  filters.pageNum = 1
  void loadDevices()
}

async function openDetail(deviceId: number) {
  detailDialogVisible.value = true
  detailLoading.value = true

  try {
    detail.value = await getDeviceDetail(deviceId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载设备详情失败')
  } finally {
    detailLoading.value = false
  }
}

function resetSaveForm() {
  saveForm.deviceId = undefined
  saveForm.deviceName = ''
  saveForm.deviceCode = ''
  saveForm.category = ''
  saveForm.location = ''
  saveForm.imageUrl = ''
  saveForm.description = ''
}

function openCreateDialog() {
  resetSaveForm()
  saveDialogVisible.value = true
}

function openEditDialog(row: DeviceItem) {
  saveForm.deviceId = row.deviceId
  saveForm.deviceName = row.deviceName
  saveForm.deviceCode = row.deviceCode
  saveForm.category = row.category
  saveForm.location = row.location
  saveForm.imageUrl = row.imageUrl ?? ''
  saveForm.description = row.description ?? ''
  saveDialogVisible.value = true
}

async function submitSave() {
  await saveFormRef.value?.validate()

  try {
    const payload = {
      deviceName: saveForm.deviceName,
      deviceCode: saveForm.deviceCode,
      category: saveForm.category,
      location: saveForm.location,
      imageUrl: saveForm.imageUrl,
      description: saveForm.description
    } satisfies SaveDevicePayload

    if (saveForm.deviceId) {
      await updateDevice(saveForm.deviceId, payload)
      ElMessage.success('设备更新成功')
    } else {
      await createDevice(payload)
      ElMessage.success('设备创建成功')
    }

    saveDialogVisible.value = false
    await loadDevices()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function handleStatusChange(row: DeviceItem, status: DeviceStatus) {
  try {
    await updateDeviceStatus(row.deviceId, { status })
    ElMessage.success('设备状态已更新')
    await loadDevices()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '状态更新失败')
  }
}

onMounted(() => {
  void loadDevices()
})
</script>

<template>
  <div class="device-page">
    <section class="hero-card">
      <span class="eyebrow">设备模块</span>
      <h2>设备管理与库存概览</h2>
      <p>
        所有已登录用户都可以浏览设备库存；管理员和超级管理员可以新增设备、编辑信息并调整生命周期状态。
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElInput v-model="filters.keyword" placeholder="按设备名称、编号、分类或位置搜索" clearable @keyup.enter="handleSearch" />
        <ElSelect v-model="filters.category" placeholder="分类" clearable>
          <ElOption v-for="category in categories" :key="category" :label="category" :value="category" />
        </ElSelect>
        <ElSelect v-model="filters.status" placeholder="状态" clearable>
          <ElOption v-for="status in deviceStatusOptions()" :key="status" :label="deviceStatusLabel(status)" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">重置</ElButton>
        <ElButton type="primary" @click="handleSearch">查询</ElButton>
        <ElButton v-if="canManage" type="success" @click="openCreateDialog">新增设备</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn label="设备" min-width="220">
          <template #default="{ row }">
            <div class="device-cell">
              <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.deviceName" class="device-thumb" />
              <div>
                <strong>{{ row.deviceName }}</strong>
                <div class="device-meta">{{ row.deviceCode }}</div>
              </div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="category" label="分类" min-width="130" />
        <ElTableColumn prop="location" label="位置" min-width="140" />
        <ElTableColumn prop="status" label="状态" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'DISABLED' ? 'info' : 'warning'">
              {{ deviceStatusLabel(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" min-width="340" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton link type="primary" @click="openDetail(row.deviceId)">详情</ElButton>
              <ElButton v-if="canManage" link type="warning" @click="openEditDialog(row)">编辑</ElButton>
              <ElDropdown v-if="canManage" @command="(status: DeviceStatus) => handleStatusChange(row, status)">
                <ElButton link type="success">修改状态</ElButton>
                <template #dropdown>
                  <ElDropdownMenu>
                    <ElDropdownItem v-for="status in deviceStatusOptions()" :key="status" :command="status">
                      {{ deviceStatusLabel(status) }}
                    </ElDropdownItem>
                  </ElDropdownMenu>
                </template>
              </ElDropdown>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="pagination-wrap">
        <ElPagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          background
          layout="total, prev, pager, next"
          :total="page.total"
          @current-change="loadDevices"
        />
      </div>
    </section>

    <ElDialog v-model="saveDialogVisible" :title="saveForm.deviceId ? '编辑设备' : '新增设备'" width="520px" @closed="resetSaveForm">
      <ElForm ref="saveFormRef" :model="saveForm" :rules="saveRules" label-position="top">
        <ElFormItem label="设备名称" prop="deviceName">
          <ElInput v-model="saveForm.deviceName" placeholder="请输入设备名称" />
        </ElFormItem>
        <ElFormItem label="设备编号" prop="deviceCode">
          <ElInput v-model="saveForm.deviceCode" placeholder="请输入设备编号" />
        </ElFormItem>
        <ElFormItem label="分类" prop="category">
          <ElInput v-model="saveForm.category" placeholder="请输入分类" />
        </ElFormItem>
        <ElFormItem label="位置" prop="location">
          <ElInput v-model="saveForm.location" placeholder="请输入存放位置" />
        </ElFormItem>
        <ElFormItem label="图片地址">
          <ElInput v-model="saveForm.imageUrl" placeholder="选填图片地址" />
        </ElFormItem>
        <ElFormItem label="描述">
          <ElInput v-model="saveForm.description" type="textarea" :rows="3" placeholder="选填描述信息" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="saveDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitSave">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="设备详情" width="560px">
      <ElSkeleton v-if="detailLoading" :rows="6" animated />
      <div v-else-if="detail" class="detail-grid">
        <img v-if="detail.imageUrl" :src="detail.imageUrl" :alt="detail.deviceName" class="detail-image" />
        <article>
          <strong>名称</strong>
          <span>{{ detail.deviceName }}</span>
        </article>
        <article>
          <strong>编号</strong>
          <span>{{ detail.deviceCode }}</span>
        </article>
        <article>
          <strong>分类</strong>
          <span>{{ detail.category }}</span>
        </article>
        <article>
          <strong>状态</strong>
          <span>{{ deviceStatusLabel(detail.status) }}</span>
        </article>
        <article>
          <strong>位置</strong>
          <span>{{ detail.location }}</span>
        </article>
        <article class="full-width">
          <strong>描述</strong>
          <span>{{ detail.description || '--' }}</span>
        </article>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.device-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.toolbar-card,
.table-card {
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

.hero-card p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: #155e75;
  background: #cffafe;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.toolbar-card {
  display: grid;
  gap: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr 1fr;
  gap: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.device-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.device-thumb {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  object-fit: cover;
  background: #e2e8f0;
}

.device-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-grid article {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.detail-grid span {
  color: #475569;
}

.detail-image {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 18px;
  background: #e2e8f0;
}

.full-width {
  grid-column: 1 / -1;
}

@media (max-width: 960px) {
  .filter-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
