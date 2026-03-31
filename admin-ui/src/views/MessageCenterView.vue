<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmMessage, listMessages, unconfirmedSummary } from '@/api/messages'
import { useAuthStore } from '@/store/auth'
import type { MessageItem, NotificationType } from '@/types/profile'
import type { MessageListQuery, MessageSummary } from '@/types/message'

const authStore = useAuthStore()
const loading = ref(false)
const messages = ref<MessageItem[]>([])
const summary = ref<MessageSummary | null>(null)

const query = reactive<MessageListQuery>({
  userId: undefined,
  type: undefined,
  confirmStatus: undefined,
  pageNum: 1,
  pageSize: 10
})

const total = ref(0)

const currentRole = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT')
const showUserFilter = computed(() => currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN')

const messageTypeOptions: NotificationType[] = [
  'FIRST_LOGIN_PASSWORD_CHANGE',
  'PASSWORD_RESET',
  'RESERVATION_EXPIRED',
  'BORROW_OVERDUE',
  'ABOUT_TO_EXPIRE_REMINDER',
  'OVERDUE_REMINDER'
]

function messageTypeLabel(type: NotificationType) {
  return type.split('_').join(' ')
}

async function loadMessages() {
  loading.value = true

  try {
    const [messageResult, summaryResult] = await Promise.all([
      listMessages(query),
      unconfirmedSummary()
    ])
    messages.value = messageResult.list
    total.value = messageResult.total
    summary.value = summaryResult
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load messages')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  void loadMessages()
}

function handleReset() {
  query.userId = undefined
  query.type = undefined
  query.confirmStatus = undefined
  query.pageNum = 1
  void loadMessages()
}

async function handleConfirm(messageId: number) {
  try {
    await confirmMessage(messageId)
    ElMessage.success('Message confirmed')
    await loadMessages()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Confirm failed')
  }
}

onMounted(() => {
  void loadMessages()
})
</script>

<template>
  <div class="message-page">
    <section class="hero-card">
      <span class="eyebrow">Message Module</span>
      <h2>Notification center and confirmation workflow</h2>
      <p>
        This page provides a dedicated message workbench for staff roles, including unread summary, filtering, and
        one-click confirmation.
      </p>
    </section>

    <section v-if="summary" class="summary-grid">
      <article class="summary-card">
        <strong>{{ summary.total }}</strong>
        <span>Total unconfirmed</span>
      </article>
      <article class="summary-card">
        <strong>{{ summary.aboutToExpireCount }}</strong>
        <span>About to expire</span>
      </article>
      <article class="summary-card">
        <strong>{{ summary.overdueCount }}</strong>
        <span>Overdue</span>
      </article>
      <article class="summary-card">
        <strong>{{ summary.firstLoginCount }}</strong>
        <span>First login alerts</span>
      </article>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElInput v-if="showUserFilter" v-model.number="query.userId" placeholder="Filter by user ID" clearable />
        <ElSelect v-model="query.type" placeholder="Message type" clearable>
          <ElOption v-for="type in messageTypeOptions" :key="type" :label="messageTypeLabel(type)" :value="type" />
        </ElSelect>
        <ElSelect v-model="query.confirmStatus" placeholder="Confirm status" clearable>
          <ElOption label="Unconfirmed" value="UNCONFIRMED" />
          <ElOption label="Confirmed" value="CONFIRMED" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Search</ElButton>
      </div>
    </section>

    <section class="panel-card" v-loading="loading">
      <div class="message-list">
        <article v-for="message in messages" :key="message.messageId" class="message-card">
          <div class="message-header">
            <div>
              <strong>{{ message.title }}</strong>
              <span>{{ messageTypeLabel(message.type) }}</span>
            </div>
            <ElTag :type="message.confirmStatus === 'CONFIRMED' ? 'success' : 'warning'">
              {{ message.confirmStatus }}
            </ElTag>
          </div>
          <p>{{ message.content }}</p>
          <div class="message-footer">
            <span>{{ message.createdAt }}</span>
            <ElButton
              v-if="message.confirmStatus === 'UNCONFIRMED'"
              type="primary"
              link
              @click="handleConfirm(message.messageId)"
            >
              Confirm
            </ElButton>
          </div>
        </article>
      </div>

      <div class="pagination-wrap">
        <ElPagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadMessages"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.message-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.toolbar-card,
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
  color: #155e75;
  background: #cffafe;
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

.toolbar-card {
  display: grid;
  gap: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
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
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .message-header,
  .message-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
