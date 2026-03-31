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

async function loadRecords() {
  loading.value = true

  try {
    const data = await listBorrowRecords(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load borrow records')
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
    ElMessage.success('Pickup confirmed')
    pickupDialogVisible.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Pickup failed')
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
    ElMessage.success('Return confirmed')
    returnDialogVisible.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Return failed')
  }
}

async function handleMarkOverdue(row: BorrowRecordItem) {
  try {
    await markBorrowRecordOverdue(row.recordId)
    ElMessage.success('Record marked overdue')
    await loadRecords()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Update failed')
  }
}

async function openReminders(type: 'ABOUT_TO_EXPIRE' | 'OVERDUE') {
  try {
    reminders.value = await listBorrowReminders(type)
    reminderDrawerVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load reminders')
  }
}

onMounted(() => {
  void loadRecords()
})
</script>

<template>
  <div class="borrow-page">
    <section class="hero-card">
      <span class="eyebrow">Borrow Module</span>
      <h2>Pickup, return, and overdue tracking</h2>
      <p>
        Students can confirm pickup and return, while admin roles can inspect reminders and mark overdue records when
        needed.
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="filters.status" placeholder="Borrow status" clearable>
          <ElOption v-for="status in statusOptions" :key="status" :label="status" :value="status" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Search</ElButton>
        <ElButton v-if="isAdmin" type="warning" @click="openReminders('ABOUT_TO_EXPIRE')">About To Expire</ElButton>
        <ElButton v-if="isAdmin" type="danger" @click="openReminders('OVERDUE')">Overdue Reminders</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn prop="deviceName" label="Device" min-width="180" />
        <ElTableColumn prop="userName" label="Borrower" min-width="140" />
        <ElTableColumn prop="status" label="Status" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.status === 'BORROWING' ? 'warning' : row.status === 'RETURNED' ? 'success' : 'info'">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="pickupTime" label="Pickup Time" min-width="160" />
        <ElTableColumn prop="expectedReturnTime" label="Expected Return" min-width="160" />
        <ElTableColumn prop="returnTime" label="Return Time" min-width="160" />
        <ElTableColumn label="Actions" min-width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton v-if="isStudent && row.status === 'PICKUP_PENDING'" link type="primary" @click="openPickupDialog(row)">
                Confirm Pickup
              </ElButton>
              <ElButton v-if="isStudent && (row.status === 'BORROWING' || row.status === 'OVERDUE')" link type="success" @click="openReturnDialog(row)">
                Confirm Return
              </ElButton>
              <ElButton v-if="isAdmin && row.status === 'BORROWING'" link type="danger" @click="handleMarkOverdue(row)">
                Mark Overdue
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

    <ElDialog v-model="pickupDialogVisible" title="Confirm Pickup" width="420px">
      <ElForm label-position="top">
        <ElFormItem label="Pickup Time">
          <ElInput v-model="pickupForm.pickupTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="pickupDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitPickup">Confirm</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="returnDialogVisible" title="Confirm Return" width="460px">
      <ElForm label-position="top">
        <ElFormItem label="Return Time">
          <ElInput v-model="returnForm.returnTime" placeholder="YYYY-MM-DD HH:mm:ss" />
        </ElFormItem>
        <ElFormItem label="Device Condition">
          <ElSelect v-model="returnForm.deviceCondition">
            <ElOption label="NORMAL" value="NORMAL" />
            <ElOption label="DAMAGED" value="DAMAGED" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="returnDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitReturn">Confirm</ElButton>
      </template>
    </ElDialog>

    <ElDrawer v-model="reminderDrawerVisible" title="Reminder Records" size="480px">
      <div class="reminder-list">
        <article v-for="item in reminders" :key="item.recordId" class="reminder-card">
          <strong>{{ item.deviceName }}</strong>
          <span>{{ item.userName }}</span>
          <span>{{ item.status }} / {{ item.expectedReturnTime }}</span>
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
