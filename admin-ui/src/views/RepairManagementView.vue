<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDevices } from '@/api/devices'
import { createRepair, getRepairDetail, listRepairs, updateRepairStatus } from '@/api/repairs'
import { useAuthStore } from '@/store/auth'
import type { DeviceItem } from '@/types/device'
import type { CreateRepairPayload, RepairItem, RepairListQuery, RepairStatus, UpdateRepairStatusPayload } from '@/types/repair'

const authStore = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const createFormRef = ref()

const devices = ref<DeviceItem[]>([])
const detail = ref<RepairItem | null>(null)

const filters = reactive<RepairListQuery>({
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const page = reactive({
  list: [] as RepairItem[],
  total: 0
})

const createForm = reactive({
  deviceId: undefined as number | undefined,
  description: ''
})

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const canCreate = computed(() => currentRole.value === 'STUDENT')
const canUpdate = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN')
const statusOptions: RepairStatus[] = ['PENDING', 'PROCESSING', 'COMPLETED', 'UNREPAIRABLE']

const createRules = {
  deviceId: [{ required: true, message: 'Please select a device', trigger: 'change' }],
  description: [{ required: true, message: 'Please describe the issue', trigger: 'blur' }]
}

async function loadRepairs() {
  loading.value = true

  try {
    const data = await listRepairs(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load repairs')
  } finally {
    loading.value = false
  }
}

async function loadDevicesForCreate() {
  try {
    const data = await listDevices({
      pageNum: 1,
      pageSize: 100
    })
    devices.value = data.list
  } catch {
    devices.value = []
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadRepairs()
}

function handleReset() {
  filters.status = undefined
  filters.pageNum = 1
  void loadRepairs()
}

function resetCreateForm() {
  createForm.deviceId = undefined
  createForm.description = ''
}

async function submitCreate() {
  await createFormRef.value?.validate()

  try {
    await createRepair({
      deviceId: createForm.deviceId!,
      description: createForm.description
    } satisfies CreateRepairPayload)
    ElMessage.success('Repair request submitted')
    createDialogVisible.value = false
    resetCreateForm()
    await loadRepairs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Create failed')
  }
}

async function openDetail(repairId: number) {
  detailDialogVisible.value = true
  detailLoading.value = true

  try {
    detail.value = await getRepairDetail(repairId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load repair detail')
  } finally {
    detailLoading.value = false
  }
}

async function handleUpdateStatus(row: RepairItem, status: RepairStatus) {
  let comment = ''

  if (status === 'UNREPAIRABLE') {
    comment = await ElMessageBox.prompt('Provide an explanation for the unrepaired result', 'Update Repair Status', {
      confirmButtonText: 'Save',
      cancelButtonText: 'Cancel',
      inputPattern: /\S+/,
      inputErrorMessage: 'Comment is required'
    }).then((result) => result.value)
  }

  try {
    await updateRepairStatus(row.repairId, {
      status,
      comment
    } satisfies UpdateRepairStatusPayload)
    ElMessage.success('Repair status updated')
    await loadRepairs()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : 'Update failed')
  }
}

onMounted(() => {
  void Promise.all([loadRepairs(), loadDevicesForCreate()])
})
</script>

<template>
  <div class="repair-page">
    <section class="hero-card">
      <span class="eyebrow">Repair Module</span>
      <h2>Repair requests and processing workflow</h2>
      <p>
        Students can submit repair requests when devices fail, and admin roles can track progress from pending to
        completion or unrepairable status.
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="filters.status" placeholder="Repair status" clearable>
          <ElOption v-for="status in statusOptions" :key="status" :label="status" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Search</ElButton>
        <ElButton v-if="canCreate" type="success" @click="createDialogVisible = true">Create Repair</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn prop="deviceName" label="Device" min-width="180" />
        <ElTableColumn prop="applicantName" label="Applicant" min-width="140" />
        <ElTableColumn prop="status" label="Status" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'UNREPAIRABLE' ? 'danger' : 'warning'">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="Created At" min-width="160" />
        <ElTableColumn prop="updatedAt" label="Updated At" min-width="160" />
        <ElTableColumn prop="description" label="Description" min-width="220" />
        <ElTableColumn label="Actions" min-width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton link type="primary" @click="openDetail(row.repairId)">Detail</ElButton>
              <ElDropdown v-if="canUpdate" @command="(status: RepairStatus) => handleUpdateStatus(row, status)">
                <ElButton link type="success">Update Status</ElButton>
                <template #dropdown>
                  <ElDropdownMenu>
                    <ElDropdownItem v-for="status in statusOptions" :key="status" :command="status">
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
          @current-change="loadRepairs"
        />
      </div>
    </section>

    <ElDialog v-model="createDialogVisible" title="Create Repair Request" width="520px" @closed="resetCreateForm">
      <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <ElFormItem label="Device" prop="deviceId">
          <ElSelect v-model="createForm.deviceId" placeholder="Select device">
            <ElOption v-for="device in devices" :key="device.deviceId" :label="`${device.deviceName} / ${device.deviceCode}`" :value="device.deviceId" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="Issue Description" prop="description">
          <ElInput v-model="createForm.description" type="textarea" :rows="4" placeholder="Describe the device issue" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitCreate">Submit</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="Repair Detail" width="560px">
      <ElSkeleton v-if="detailLoading" :rows="7" animated />
      <div v-else-if="detail" class="detail-grid">
        <article><strong>Device</strong><span>{{ detail.deviceName }}</span></article>
        <article><strong>Applicant</strong><span>{{ detail.applicantName }}</span></article>
        <article><strong>Status</strong><span>{{ detail.status }}</span></article>
        <article><strong>Created At</strong><span>{{ detail.createdAt }}</span></article>
        <article><strong>Updated At</strong><span>{{ detail.updatedAt }}</span></article>
        <article class="full-width"><strong>Description</strong><span>{{ detail.description }}</span></article>
        <article class="full-width"><strong>Comment</strong><span>{{ detail.comment || '--' }}</span></article>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.repair-page {
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
  grid-template-columns: 280px;
  gap: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
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
