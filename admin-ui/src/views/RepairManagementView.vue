<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyBorrowRecords } from '@/api/profile'
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
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  description: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

function repairStatusLabel(status: RepairStatus) {
  switch (status) {
    case 'PENDING':
      return '待处理'
    case 'PROCESSING':
      return '处理中'
    case 'COMPLETED':
      return '已完成'
    case 'UNREPAIRABLE':
      return '无法修复'
    default:
      return status
  }
}

async function loadRepairs() {
  loading.value = true

  try {
    const data = await listRepairs(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载维修记录失败')
  } finally {
    loading.value = false
  }
}

async function loadDevicesForCreate() {
  try {
    const [devicePage, borrowPage] = await Promise.all([
      listDevices({
        pageNum: 1,
        pageSize: 100
      }),
      canCreate.value
        ? listMyBorrowRecords({
            status: undefined,
            pageNum: 1,
            pageSize: 100
          })
        : Promise.resolve({ list: [] })
    ])

    if (!canCreate.value) {
      devices.value = devicePage.list
      return
    }

    const borrowingDeviceIds = new Set(
      borrowPage.list
        .filter((item) => item.status === 'BORROWING' || item.status === 'OVERDUE')
        .map((item) => item.deviceId)
    )
    devices.value = devicePage.list.filter((item) => borrowingDeviceIds.has(item.deviceId))
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
    ElMessage.success('维修申请已提交')
    createDialogVisible.value = false
    resetCreateForm()
    await loadRepairs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function openDetail(repairId: number) {
  detailDialogVisible.value = true
  detailLoading.value = true

  try {
    detail.value = await getRepairDetail(repairId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载维修详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function handleUpdateStatus(row: RepairItem, status: RepairStatus) {
  let comment = ''

  if (status === 'UNREPAIRABLE') {
    comment = await ElMessageBox.prompt('请输入无法修复的说明', '更新维修状态', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '请输入说明'
    }).then((result) => result.value)
  }

  try {
    await updateRepairStatus(row.repairId, {
      status,
      comment
    } satisfies UpdateRepairStatusPayload)
    ElMessage.success('维修状态已更新')
    await loadRepairs()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

onMounted(() => {
  void Promise.all([loadRepairs(), loadDevicesForCreate()])
})
</script>

<template>
  <div class="repair-page">
    <section class="hero-card">
      <span class="eyebrow">维修模块</span>
      <h2>维修申请与处理流程</h2>
      <p>
        学生仅可为自己当前借用中的设备发起报修，管理员可跟踪并处理从待处理到完成或无法修复的全过程。
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="filters.status" placeholder="维修状态" clearable>
          <ElOption v-for="status in statusOptions" :key="status" :label="repairStatusLabel(status)" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">重置</ElButton>
        <ElButton type="primary" @click="handleSearch">查询</ElButton>
        <ElButton v-if="canCreate" type="success" @click="createDialogVisible = true">发起报修</ElButton>
      </div>
    </section>

    <section class="table-card">
      <div class="table-scroll">
        <ElTable :data="page.list" v-loading="loading" width="100%">
          <ElTableColumn prop="deviceName" label="设备" min-width="180" />
          <ElTableColumn prop="applicantName" label="申请人" min-width="140" />
          <ElTableColumn prop="status" label="状态" min-width="140">
            <template #default="{ row }">
              <ElTag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'UNREPAIRABLE' ? 'danger' : 'warning'">
                {{ repairStatusLabel(row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="createdAt" label="创建时间" min-width="160" />
          <ElTableColumn prop="updatedAt" label="更新时间" min-width="160" />
          <ElTableColumn prop="description" label="故障描述" min-width="220" />
          <ElTableColumn label="操作" min-width="320" fixed="right">
            <template #default="{ row }">
              <div class="action-row">
                <ElButton link type="primary" @click="openDetail(row.repairId)">详情</ElButton>
                <ElDropdown v-if="canUpdate" @command="(status: RepairStatus) => handleUpdateStatus(row, status)">
                  <ElButton link type="success">更新状态</ElButton>
                  <template #dropdown>
                    <ElDropdownMenu>
                      <ElDropdownItem v-for="status in statusOptions" :key="status" :command="status">
                        {{ repairStatusLabel(status) }}
                      </ElDropdownItem>
                    </ElDropdownMenu>
                  </template>
                </ElDropdown>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>

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

    <ElDialog v-model="createDialogVisible" title="发起报修" width="520px" @closed="resetCreateForm">
        <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <ElFormItem label="设备" prop="deviceId">
          <ElSelect v-model="createForm.deviceId" placeholder="请选择正在借用中的设备">
            <ElOption v-for="device in devices" :key="device.deviceId" :label="`${device.deviceName} / ${device.deviceCode}`" :value="device.deviceId" />
          </ElSelect>
        </ElFormItem>
        <p v-if="canCreate && devices.length === 0" class="dialog-tip">
          当前没有可报修的设备。只有处于借用中或已逾期但仍未归还的设备，才可以提交报修申请。
        </p>
        <ElFormItem label="故障描述" prop="description">
          <ElInput v-model="createForm.description" type="textarea" :rows="4" placeholder="请描述设备故障情况" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitCreate">提交</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="维修详情" width="560px">
      <ElSkeleton v-if="detailLoading" :rows="7" animated />
      <div v-else-if="detail" class="detail-grid">
        <article><strong>设备</strong><span>{{ detail.deviceName }}</span></article>
        <article><strong>申请人</strong><span>{{ detail.applicantName }}</span></article>
        <article><strong>状态</strong><span>{{ repairStatusLabel(detail.status) }}</span></article>
        <article><strong>创建时间</strong><span>{{ detail.createdAt }}</span></article>
        <article><strong>更新时间</strong><span>{{ detail.updatedAt }}</span></article>
        <article class="full-width"><strong>故障描述</strong><span>{{ detail.description }}</span></article>
        <article class="full-width"><strong>备注</strong><span>{{ detail.comment || '--' }}</span></article>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.repair-page {
  display: grid;
  gap: 20px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

.hero-card,
.toolbar-card,
.table-card {
  width: 100%;
  max-width: 100%;
  min-width: 0;
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
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
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
  flex-wrap: wrap;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-scroll {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 1120px;
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

.dialog-tip {
  margin: 0 0 14px;
  color: #b45309;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .filter-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .toolbar-card,
  .table-card {
    padding: 18px;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .pagination-wrap {
    justify-content: flex-start;
  }
}
</style>


