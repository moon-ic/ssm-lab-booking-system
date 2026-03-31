<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDevices } from '@/api/devices'
import {
  approveReservation,
  cancelReservation,
  createReservation,
  getReservationDetail,
  listReservations
} from '@/api/reservations'
import { useAuthStore } from '@/store/auth'
import type { DeviceItem } from '@/types/device'
import type {
  ApproveReservationPayload,
  CreateReservationPayload,
  ReservationItem,
  ReservationListQuery,
  ReservationStatus
} from '@/types/reservation'

const authStore = useAuthStore()
const loading = ref(false)
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const createFormRef = ref()
const detailLoading = ref(false)

const devices = ref<DeviceItem[]>([])
const detail = ref<ReservationItem | null>(null)

const filters = reactive<ReservationListQuery>({
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const page = reactive({
  list: [] as ReservationItem[],
  total: 0
})

const createForm = reactive({
  deviceId: undefined as number | undefined,
  startTime: '',
  endTime: '',
  purpose: ''
})

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const canCreate = computed(() => currentRole.value === 'STUDENT')
const canReview = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN' || currentRole.value === 'TEACHER')
const statusOptions: ReservationStatus[] = ['PENDING', 'PICKUP_PENDING', 'REJECTED', 'EXPIRED', 'CANCELLED']

const createRules = {
  deviceId: [{ required: true, message: 'Please select a device', trigger: 'change' }],
  startTime: [{ required: true, message: 'Please enter start time', trigger: 'blur' }],
  endTime: [{ required: true, message: 'Please enter end time', trigger: 'blur' }],
  purpose: [{ required: true, message: 'Please enter purpose', trigger: 'blur' }]
}

async function loadReservations() {
  loading.value = true

  try {
    const data = await listReservations(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load reservations')
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
    devices.value = data.list.filter((item) => item.status === 'AVAILABLE')
  } catch {
    devices.value = []
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadReservations()
}

function handleReset() {
  filters.status = undefined
  filters.pageNum = 1
  void loadReservations()
}

function resetCreateForm() {
  createForm.deviceId = undefined
  createForm.startTime = ''
  createForm.endTime = ''
  createForm.purpose = ''
}

async function submitCreate() {
  await createFormRef.value?.validate()

  try {
    await createReservation({
      deviceId: createForm.deviceId!,
      startTime: createForm.startTime,
      endTime: createForm.endTime,
      purpose: createForm.purpose
    } satisfies CreateReservationPayload)
    ElMessage.success('Reservation submitted')
    createDialogVisible.value = false
    resetCreateForm()
    await loadReservations()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Create failed')
  }
}

async function openDetail(reservationId: number) {
  detailDialogVisible.value = true
  detailLoading.value = true

  try {
    detail.value = await getReservationDetail(reservationId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load details')
  } finally {
    detailLoading.value = false
  }
}

async function handleReview(row: ReservationItem, action: 'APPROVE' | 'REJECT') {
  let comment = ''

  if (action === 'REJECT') {
    comment = await ElMessageBox.prompt('Provide the rejection reason', 'Reject reservation', {
      confirmButtonText: 'Reject',
      cancelButtonText: 'Cancel',
      inputPattern: /\S+/,
      inputErrorMessage: 'Comment is required'
    }).then((result) => result.value)
  }

  try {
    await approveReservation(row.reservationId, {
      action,
      comment
    } satisfies ApproveReservationPayload)
    ElMessage.success(action === 'APPROVE' ? 'Reservation approved' : 'Reservation rejected')
    await loadReservations()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : 'Review failed')
  }
}

async function handleCancel(row: ReservationItem) {
  try {
    await ElMessageBox.confirm('Cancel this pending reservation?', 'Cancel reservation', {
      confirmButtonText: 'Cancel reservation',
      cancelButtonText: 'Back',
      type: 'warning'
    })
    await cancelReservation(row.reservationId)
    ElMessage.success('Reservation cancelled')
    await loadReservations()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : 'Cancel failed')
  }
}

onMounted(() => {
  void Promise.all([loadReservations(), loadDevicesForCreate()])
})
</script>

<template>
  <div class="reservation-page">
    <section class="hero-card">
      <span class="eyebrow">Reservation Module</span>
      <h2>Reservation requests and approval workflow</h2>
      <p>
        Students can submit reservations, while admin and teacher roles can review visible requests. The page follows
        the backend status flow from pending to pickup pending or rejected.
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="filters.status" placeholder="Reservation status" clearable>
          <ElOption v-for="status in statusOptions" :key="status" :label="status" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Search</ElButton>
        <ElButton v-if="canCreate" type="success" @click="createDialogVisible = true">Create Reservation</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn prop="deviceName" label="Device" min-width="180" />
        <ElTableColumn prop="applicantName" label="Applicant" min-width="140" />
        <ElTableColumn prop="startTime" label="Start Time" min-width="160" />
        <ElTableColumn prop="endTime" label="End Time" min-width="160" />
        <ElTableColumn prop="status" label="Status" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.status === 'PENDING' ? 'warning' : row.status === 'PICKUP_PENDING' ? 'success' : 'info'">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="purpose" label="Purpose" min-width="220" />
        <ElTableColumn label="Actions" min-width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton link type="primary" @click="openDetail(row.reservationId)">Detail</ElButton>
              <ElButton
                v-if="canReview && row.status === 'PENDING'"
                link
                type="success"
                @click="handleReview(row, 'APPROVE')"
              >
                Approve
              </ElButton>
              <ElButton
                v-if="canReview && row.status === 'PENDING'"
                link
                type="danger"
                @click="handleReview(row, 'REJECT')"
              >
                Reject
              </ElButton>
              <ElButton
                v-if="currentRole === 'STUDENT' && row.status === 'PENDING'"
                link
                type="warning"
                @click="handleCancel(row)"
              >
                Cancel
              </ElButton>
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
          @current-change="loadReservations"
        />
      </div>
    </section>

    <ElDialog v-model="createDialogVisible" title="Create Reservation" width="520px" @closed="resetCreateForm">
      <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <ElFormItem label="Device" prop="deviceId">
          <ElSelect v-model="createForm.deviceId" placeholder="Select available device">
            <ElOption v-for="device in devices" :key="device.deviceId" :label="`${device.deviceName} / ${device.deviceCode}`" :value="device.deviceId" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="Start Time" prop="startTime">
          <ElInput v-model="createForm.startTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </ElFormItem>
        <ElFormItem label="End Time" prop="endTime">
          <ElInput v-model="createForm.endTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </ElFormItem>
        <ElFormItem label="Purpose" prop="purpose">
          <ElInput v-model="createForm.purpose" type="textarea" :rows="3" placeholder="Describe the purpose" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitCreate">Submit</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="Reservation Detail" width="560px">
      <ElSkeleton v-if="detailLoading" :rows="7" animated />
      <div v-else-if="detail" class="detail-grid">
        <article><strong>Device</strong><span>{{ detail.deviceName }}</span></article>
        <article><strong>Applicant</strong><span>{{ detail.applicantName }}</span></article>
        <article><strong>Start</strong><span>{{ detail.startTime }}</span></article>
        <article><strong>End</strong><span>{{ detail.endTime }}</span></article>
        <article><strong>Status</strong><span>{{ detail.status }}</span></article>
        <article><strong>Created At</strong><span>{{ detail.createdAt || '--' }}</span></article>
        <article class="full-width"><strong>Purpose</strong><span>{{ detail.purpose }}</span></article>
        <article class="full-width"><strong>Review Comment</strong><span>{{ detail.reviewComment || '--' }}</span></article>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.reservation-page {
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
