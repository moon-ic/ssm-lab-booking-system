<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listBorrowRecords,
  listBorrowReminders,
  markBorrowRecordOverdue,
  pickupBorrowRecord,
  returnBorrowRecord
} from '@/api/borrow-records'
import { useAuthStore } from '@/store/auth'
import type { BorrowRecordItem, BorrowRecordListQuery, BorrowStatus } from '@/types/borrow-record'

const authStore = useAuthStore()
const loading = ref(false)
const pickupDialogVisible = ref(false)
const returnDialogVisible = ref(false)
const reminderDrawerVisible = ref(false)
const currentRecord = ref<BorrowRecordItem | null>(null)
const reminders = ref<Array<BorrowRecordItem & { reminderType: string }>>([])

const filters = reactive<BorrowRecordListQuery>({
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const page = reactive({
  list: [] as BorrowRecordItem[],
  total: 0
})

const pickupForm = reactive({
  pickupTime: ''
})

const returnForm = reactive({
  returnTime: '',
  deviceCondition: 'NORMAL'
})

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const isStudent = computed(() => currentRole.value === 'STUDENT')
const isAdmin = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN')
const statusOptions: BorrowStatus[] = ['PICKUP_PENDING', 'BORROWING', 'RETURNED', 'OVERDUE']

function borrowStatusLabel(status: BorrowStatus) {
  switch (status) {
    case 'PICKUP_PENDING':
      return '待领取'
    case 'BORROWING':
      return '借用中'
    case 'RETURNED':
      return '已归还'
    case 'OVERDUE':
      return '已逾期'
    default:
      return status
  }
}

function deviceConditionLabel(condition: string) {
  return condition === 'DAMAGED' ? '损坏' : '正常'
}

async function loadRecords() {
  loading.value = true

  try {
    const data = await listBorrowRecords(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载借用记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadRecords()
}

function handleReset() {
  filters.status = undefined
  filters.pageNum = 1
  void loadRecords()
}

function openPickupDialog(row: BorrowRecordItem) {
  currentRecord.value = row
  pickupForm.pickupTime = ''
  pickupDialogVisible.value = true
}

function openReturnDialog(row: BorrowRecordItem) {
  currentRecord.value = row
  returnForm.returnTime = ''
  returnForm.deviceCondition = 'NORMAL'
  returnDialogVisible.value = true
}

async function submitPickup() {
  if (!currentRecord.value) {
    return
  }

  try {
    await pickupBorrowRecord(currentRecord.value.recordId, {
      pickupTime: pickupForm.pickupTime
    })
    ElMessage.success('已确认领取')
    pickupDialogVisible.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认领取失败')
  }
}

async function submitReturn() {
  if (!currentRecord.value) {
    return
  }

  try {
    await returnBorrowRecord(currentRecord.value.recordId, {
      returnTime: returnForm.returnTime,
      deviceCondition: returnForm.deviceCondition
    })
    ElMessage.success('已确认归还')
    returnDialogVisible.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认归还失败')
  }
}

async function handleMarkOverdue(row: BorrowRecordItem) {
  try {
    await markBorrowRecordOverdue(row.recordId)
    ElMessage.success('记录已标记为逾期')
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

async function openReminders(type: 'ABOUT_TO_EXPIRE' | 'OVERDUE') {
  try {
    reminders.value = await listBorrowReminders(type)
    reminderDrawerVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载提醒记录失败')
  }
}

onMounted(() => {
  void loadRecords()
})
</script>

<template>
  <div class="borrow-page">
    <section class="hero-card">
      <span class="eyebrow">借用模块</span>
      <h2>领取、归还与逾期跟踪</h2>
      <p>
        学生可以确认领取和归还，管理员可以查看提醒记录，并在需要时将记录标记为逾期。
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="filters.status" placeholder="借用状态" clearable>
          <ElOption v-for="status in statusOptions" :key="status" :label="borrowStatusLabel(status)" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">重置</ElButton>
        <ElButton type="primary" @click="handleSearch">查询</ElButton>
        <ElButton v-if="isAdmin" type="warning" @click="openReminders('ABOUT_TO_EXPIRE')">即将到期</ElButton>
        <ElButton v-if="isAdmin" type="danger" @click="openReminders('OVERDUE')">逾期提醒</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn prop="deviceName" label="设备" min-width="180" />
        <ElTableColumn prop="userName" label="借用人" min-width="140" />
        <ElTableColumn prop="status" label="状态" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.status === 'BORROWING' ? 'warning' : row.status === 'RETURNED' ? 'success' : 'info'">
              {{ borrowStatusLabel(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="pickupTime" label="领取时间" min-width="160" />
        <ElTableColumn prop="expectedReturnTime" label="应还时间" min-width="160" />
        <ElTableColumn prop="returnTime" label="归还时间" min-width="160" />
        <ElTableColumn label="操作" min-width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton v-if="isStudent && row.status === 'PICKUP_PENDING'" link type="primary" @click="openPickupDialog(row)">
                确认领取
              </ElButton>
              <ElButton v-if="isStudent && (row.status === 'BORROWING' || row.status === 'OVERDUE')" link type="success" @click="openReturnDialog(row)">
                确认归还
              </ElButton>
              <ElButton v-if="isAdmin && row.status === 'BORROWING'" link type="danger" @click="handleMarkOverdue(row)">
                标记逾期
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
          @current-change="loadRecords"
        />
      </div>
    </section>

    <ElDialog v-model="pickupDialogVisible" title="确认领取" width="420px">
      <ElForm label-position="top">
        <ElFormItem label="领取时间">
          <ElInput v-model="pickupForm.pickupTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="pickupDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitPickup">确认</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="returnDialogVisible" title="确认归还" width="460px">
      <ElForm label-position="top">
        <ElFormItem label="归还时间">
          <ElInput v-model="returnForm.returnTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </ElFormItem>
        <ElFormItem label="设备状况">
          <ElSelect v-model="returnForm.deviceCondition">
            <ElOption label="正常" value="NORMAL" />
            <ElOption label="损坏" value="DAMAGED" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="returnDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitReturn">确认</ElButton>
      </template>
    </ElDialog>

    <ElDrawer v-model="reminderDrawerVisible" title="提醒记录" size="480px">
      <div class="reminder-list">
        <article v-for="item in reminders" :key="item.recordId" class="reminder-card">
          <strong>{{ item.deviceName }}</strong>
          <span>{{ item.userName }}</span>
          <span>{{ borrowStatusLabel(item.status) }} / {{ item.expectedReturnTime }}</span>
        </article>
      </div>
    </ElDrawer>
  </div>
</template>

<style scoped>
.borrow-page {
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

.reminder-list {
  display: grid;
  gap: 12px;
}

.reminder-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.reminder-card span {
  color: #475569;
}

@media (max-width: 960px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
