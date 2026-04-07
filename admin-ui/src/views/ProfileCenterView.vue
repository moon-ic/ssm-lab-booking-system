<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  confirmMyMessage,
  getProfile,
  listMyBorrowRecords,
  listMyMessages
} from '@/api/profile'
import type {
  BorrowRecordItem,
  BorrowRecordQuery,
  BorrowStatus,
  MessageItem,
  MessageQuery,
  NotificationType,
  ProfileSummary
} from '@/types/profile'

const loading = ref(false)
const profile = ref<ProfileSummary | null>(null)
const borrowData = ref<BorrowRecordItem[]>([])
const messageData = ref<MessageItem[]>([])
const activeTab = ref<'profile' | 'borrow-records' | 'messages'>('profile')

const borrowQuery = reactive<BorrowRecordQuery>({
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const messageQuery = reactive<MessageQuery>({
  confirmStatus: undefined,
  type: undefined,
  pageNum: 1,
  pageSize: 10
})

const borrowTotal = ref(0)
const messageTotal = ref(0)

const borrowStatusOptions: BorrowStatus[] = ['PICKUP_PENDING', 'BORROWING', 'RETURNED', 'OVERDUE']
const messageTypeOptions: NotificationType[] = [
  'FIRST_LOGIN_PASSWORD_CHANGE',
  'PASSWORD_RESET',
  'RESERVATION_EXPIRED',
  'BORROW_OVERDUE',
  'ABOUT_TO_EXPIRE_REMINDER',
  'OVERDUE_REMINDER'
]

const unconfirmedCount = computed(() => messageData.value.filter((item) => item.confirmStatus === 'UNCONFIRMED').length)

function roleLabel(roleCode?: string) {
  switch (roleCode) {
    case 'SUPER_ADMIN':
      return '超级管理员'
    case 'ADMIN':
      return '管理员'
    case 'TEACHER':
      return '教师'
    case 'STUDENT':
      return '学生'
    default:
      return '--'
  }
}

function messageTypeLabel(type: NotificationType) {
  switch (type) {
    case 'FIRST_LOGIN_PASSWORD_CHANGE':
      return '首次登录改密提醒'
    case 'PASSWORD_RESET':
      return '密码重置通知'
    case 'RESERVATION_EXPIRED':
      return '预约已过期'
    case 'BORROW_OVERDUE':
      return '借用逾期'
    case 'ABOUT_TO_EXPIRE_REMINDER':
      return '即将到期提醒'
    case 'OVERDUE_REMINDER':
      return '逾期提醒'
    default:
      return type
  }
}

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

function confirmStatusLabel(status: 'UNCONFIRMED' | 'CONFIRMED') {
  return status === 'CONFIRMED' ? '已确认' : '未确认'
}

async function loadProfile() {
  profile.value = await getProfile()
}

async function loadBorrowRecords() {
  const data = await listMyBorrowRecords(borrowQuery)
  borrowData.value = data.list
  borrowTotal.value = data.total
}

async function loadMessages() {
  const data = await listMyMessages(messageQuery)
  messageData.value = data.list
  messageTotal.value = data.total
}

async function loadAll() {
  loading.value = true

  try {
    await Promise.all([loadProfile(), loadBorrowRecords(), loadMessages()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载个人中心失败')
  } finally {
    loading.value = false
  }
}

async function handleConfirmMessage(messageId: number) {
  try {
    await confirmMyMessage(messageId)
    ElMessage.success('消息已确认')
    await loadMessages()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认消息失败')
  }
}

function handleBorrowFilter() {
  borrowQuery.pageNum = 1
  void loadBorrowRecords()
}

function handleMessageFilter() {
  messageQuery.pageNum = 1
  void loadMessages()
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <section class="hero-card">
      <span class="eyebrow">个人中心</span>
      <h2>个人资料、借用记录与消息中心</h2>
      <p>
        该模块聚合了当前用户资料、自助借用历史和消息确认能力，所有已登录角色都可使用。
      </p>
    </section>

    <section class="summary-grid" v-if="profile">
      <article class="summary-card">
        <strong>{{ profile.name }}</strong>
        <span>{{ roleLabel(profile.roleCode) }}</span>
      </article>
      <article class="summary-card">
        <strong>{{ profile.jobNoOrStudentNo }}</strong>
        <span>登录编号</span>
      </article>
      <article class="summary-card">
        <strong>{{ profile.creditScore }}</strong>
        <span>信用分</span>
      </article>
      <article class="summary-card">
        <strong>{{ unconfirmedCount }}</strong>
        <span>未确认消息</span>
      </article>
    </section>

    <section class="panel-card">
      <ElTabs v-model="activeTab">
        <ElTabPane label="个人资料" name="profile">
          <div v-if="profile" class="profile-grid">
            <article>
              <strong>姓名</strong>
              <span>{{ profile.name }}</span>
            </article>
            <article>
              <strong>账号</strong>
              <span>{{ profile.account }}</span>
            </article>
            <article>
              <strong>登录编号</strong>
              <span>{{ profile.jobNoOrStudentNo }}</span>
            </article>
            <article>
              <strong>角色</strong>
              <span>{{ roleLabel(profile.roleCode) }}</span>
            </article>
            <article>
              <strong>状态</strong>
              <span>{{ profile.status }}</span>
            </article>
            <article>
              <strong>首次登录</strong>
              <span>{{ profile.firstLoginRequired ? '待修改密码' : '已完成' }}</span>
            </article>
          </div>
        </ElTabPane>

        <ElTabPane label="我的借用记录" name="borrow-records">
          <div class="toolbar">
            <ElSelect v-model="borrowQuery.status" placeholder="借用状态" clearable @change="handleBorrowFilter">
              <ElOption v-for="status in borrowStatusOptions" :key="status" :label="borrowStatusLabel(status)" :value="status" />
            </ElSelect>
          </div>

          <ElTable :data="borrowData">
            <ElTableColumn prop="deviceName" label="设备" min-width="180" />
            <ElTableColumn prop="status" label="状态" min-width="120">
              <template #default="{ row }">
                {{ borrowStatusLabel(row.status) }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="pickupTime" label="领取时间" min-width="160" />
            <ElTableColumn prop="expectedReturnTime" label="应还时间" min-width="160" />
            <ElTableColumn prop="returnTime" label="归还时间" min-width="160" />
            <ElTableColumn prop="deviceCondition" label="设备状况" min-width="140" />
          </ElTable>

          <div class="pagination-wrap">
            <ElPagination
              v-model:current-page="borrowQuery.pageNum"
              v-model:page-size="borrowQuery.pageSize"
              background
              layout="total, prev, pager, next"
              :total="borrowTotal"
              @current-change="loadBorrowRecords"
            />
          </div>
        </ElTabPane>

        <ElTabPane label="我的消息" name="messages">
          <div class="toolbar two-col">
            <ElSelect v-model="messageQuery.confirmStatus" placeholder="确认状态" clearable @change="handleMessageFilter">
              <ElOption label="未确认" value="UNCONFIRMED" />
              <ElOption label="已确认" value="CONFIRMED" />
            </ElSelect>
            <ElSelect v-model="messageQuery.type" placeholder="消息类型" clearable @change="handleMessageFilter">
              <ElOption v-for="type in messageTypeOptions" :key="type" :label="messageTypeLabel(type)" :value="type" />
            </ElSelect>
          </div>

          <div class="message-list">
            <article v-for="message in messageData" :key="message.messageId" class="message-card">
              <div class="message-header">
                <div>
                  <strong>{{ message.title }}</strong>
                  <span>{{ messageTypeLabel(message.type) }}</span>
                </div>
                <ElTag :type="message.confirmStatus === 'CONFIRMED' ? 'success' : 'warning'">
                  {{ confirmStatusLabel(message.confirmStatus) }}
                </ElTag>
              </div>
              <p>{{ message.content }}</p>
              <div class="message-footer">
                <span>{{ message.createdAt }}</span>
                <ElButton
                  v-if="message.confirmStatus === 'UNCONFIRMED'"
                  type="primary"
                  link
                  @click="handleConfirmMessage(message.messageId)"
                >
                  确认
                </ElButton>
              </div>
            </article>
          </div>

          <div class="pagination-wrap">
            <ElPagination
              v-model:current-page="messageQuery.pageNum"
              v-model:page-size="messageQuery.pageSize"
              background
              layout="total, prev, pager, next"
              :total="messageTotal"
              @current-change="loadMessages"
            />
          </div>
        </ElTabPane>
      </ElTabs>
    </section>
  </div>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.panel-card,
.summary-card {
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: grid;
  gap: 8px;
}

.summary-card span {
  color: #64748b;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.profile-grid article {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.profile-grid span {
  color: #475569;
}

.toolbar {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.toolbar.two-col {
  grid-template-columns: repeat(2, minmax(0, 240px));
}

.message-list {
  display: grid;
  gap: 14px;
}

.message-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.message-header,
.message-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.message-header span,
.message-footer span {
  color: #64748b;
  font-size: 13px;
}

.message-card p {
  margin: 0;
  line-height: 1.7;
  color: #334155;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 980px) {
  .summary-grid,
  .profile-grid,
  .toolbar.two-col {
    grid-template-columns: 1fr;
  }

  .message-header,
  .message-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>


