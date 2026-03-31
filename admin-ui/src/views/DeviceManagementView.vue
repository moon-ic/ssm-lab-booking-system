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
  deviceName: [{ required: true, message: 'Please enter device name', trigger: 'blur' }],
  deviceCode: [{ required: true, message: 'Please enter device code', trigger: 'blur' }],
  category: [{ required: true, message: 'Please enter category', trigger: 'blur' }],
  location: [{ required: true, message: 'Please enter location', trigger: 'blur' }]
}

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const canManage = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN')
const categories = computed(() => [...new Set(page.list.map((item) => item.category))])

async function loadDevices() {
  loading.value = true

  try {
    const data = await listDevices(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load devices')
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
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load device detail')
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
      ElMessage.success('Device updated')
    } else {
      await createDevice(payload)
      ElMessage.success('Device created')
    }

    saveDialogVisible.value = false
    await loadDevices()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Save failed')
  }
}

async function handleStatusChange(row: DeviceItem, status: DeviceStatus) {
  try {
    await updateDeviceStatus(row.deviceId, { status })
    ElMessage.success('Device status updated')
    await loadDevices()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Status update failed')
  }
}

onMounted(() => {
  void loadDevices()
})
</script>

<template>
  <div class="device-page">
    <section class="hero-card">
      <span class="eyebrow">Device Module</span>
      <h2>Device management and inventory overview</h2>
      <p>
        All signed-in users can browse device inventory. Admin and super admin can create devices, edit metadata, and
        change lifecycle status.
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElInput v-model="filters.keyword" placeholder="Search by device name, code, category, or location" clearable @keyup.enter="handleSearch" />
        <ElSelect v-model="filters.category" placeholder="Category" clearable>
          <ElOption v-for="category in categories" :key="category" :label="category" :value="category" />
        </ElSelect>
        <ElSelect v-model="filters.status" placeholder="Status" clearable>
          <ElOption v-for="status in deviceStatusOptions()" :key="status" :label="status" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Search</ElButton>
        <ElButton v-if="canManage" type="success" @click="openCreateDialog">Create Device</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn label="Device" min-width="220">
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
        <ElTableColumn prop="category" label="Category" min-width="130" />
        <ElTableColumn prop="location" label="Location" min-width="140" />
        <ElTableColumn prop="status" label="Status" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'DISABLED' ? 'info' : 'warning'">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="Actions" min-width="340" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton link type="primary" @click="openDetail(row.deviceId)">Detail</ElButton>
              <ElButton v-if="canManage" link type="warning" @click="openEditDialog(row)">Edit</ElButton>
              <ElDropdown v-if="canManage" @command="(status: DeviceStatus) => handleStatusChange(row, status)">
                <ElButton link type="success">Change Status</ElButton>
                <template #dropdown>
                  <ElDropdownMenu>
                    <ElDropdownItem v-for="status in deviceStatusOptions()" :key="status" :command="status">
                      {{ status }}
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

    <ElDialog v-model="saveDialogVisible" :title="saveForm.deviceId ? 'Edit Device' : 'Create Device'" width="520px" @closed="resetSaveForm">
      <ElForm ref="saveFormRef" :model="saveForm" :rules="saveRules" label-position="top">
        <ElFormItem label="Device Name" prop="deviceName">
          <ElInput v-model="saveForm.deviceName" placeholder="Enter device name" />
        </ElFormItem>
        <ElFormItem label="Device Code" prop="deviceCode">
          <ElInput v-model="saveForm.deviceCode" placeholder="Enter device code" />
        </ElFormItem>
        <ElFormItem label="Category" prop="category">
          <ElInput v-model="saveForm.category" placeholder="Enter category" />
        </ElFormItem>
        <ElFormItem label="Location" prop="location">
          <ElInput v-model="saveForm.location" placeholder="Enter location" />
        </ElFormItem>
        <ElFormItem label="Image URL">
          <ElInput v-model="saveForm.imageUrl" placeholder="Optional image URL" />
        </ElFormItem>
        <ElFormItem label="Description">
          <ElInput v-model="saveForm.description" type="textarea" :rows="3" placeholder="Optional description" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="saveDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitSave">Save</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="Device Detail" width="560px">
      <ElSkeleton v-if="detailLoading" :rows="6" animated />
      <div v-else-if="detail" class="detail-grid">
        <img v-if="detail.imageUrl" :src="detail.imageUrl" :alt="detail.deviceName" class="detail-image" />
        <article>
          <strong>Name</strong>
          <span>{{ detail.deviceName }}</span>
        </article>
        <article>
          <strong>Code</strong>
          <span>{{ detail.deviceCode }}</span>
        </article>
        <article>
          <strong>Category</strong>
          <span>{{ detail.category }}</span>
        </article>
        <article>
          <strong>Status</strong>
          <span>{{ detail.status }}</span>
        </article>
        <article>
          <strong>Location</strong>
          <span>{{ detail.location }}</span>
        </article>
        <article class="full-width">
          <strong>Description</strong>
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
