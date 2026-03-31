<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deviceDamageStatistics,
  hotDevices,
  statisticsOverview,
  userViolationStatistics
} from '@/api/statistics'
import type {
  DamageDeviceStatItem,
  HotDeviceStatItem,
  RankScope,
  StatisticsOverview,
  StatisticsQuery,
  UserViolationStatItem
} from '@/types/statistics'

const loading = ref(false)
const overview = ref<StatisticsOverview | null>(null)
const hotDeviceList = ref<HotDeviceStatItem[]>([])
const damageDeviceList = ref<DamageDeviceStatItem[]>([])
const violationUserList = ref<UserViolationStatItem[]>([])

const query = reactive<Required<Pick<StatisticsQuery, 'rankScope' | 'topN'>>>({
  rankScope: 'TOTAL',
  topN: 5
})

const scopeOptions: Array<{ label: string; value: RankScope }> = [
  { label: 'Total', value: 'TOTAL' },
  { label: 'Half Year', value: 'HALF_YEAR' },
  { label: 'Month', value: 'MONTH' }
]

const topNOptions = [3, 5, 10]

const summaryCards = computed(() => {
  if (!overview.value) {
    return []
  }

  return [
    {
      title: 'Devices in system',
      value: overview.value.deviceTotal,
      description: 'Registered devices across the current workspace.'
    },
    {
      title: 'Currently available',
      value: overview.value.availableDeviceTotal,
      description: 'Devices ready to be reserved or borrowed.'
    },
    {
      title: 'Active borrowing',
      value: overview.value.borrowingTotal,
      description: 'Records still in borrowing or overdue status.'
    },
    {
      title: 'Pending reservations',
      value: overview.value.pendingReservationTotal,
      description: 'Reservation requests waiting for review.'
    },
    {
      title: 'Pending repairs',
      value: overview.value.pendingRepairTotal,
      description: 'Repair requests that still need processing.'
    }
  ]
})

function scopeLabel(scope: RankScope) {
  const option = scopeOptions.find((item) => item.value === scope)
  return option?.label ?? scope
}

function statusTagType(status: string) {
  if (status === 'REPAIRING' || status === 'PROCESSING') {
    return 'warning'
  }
  if (status === 'DAMAGED' || status === 'UNREPAIRABLE' || status === 'OVERDUE') {
    return 'danger'
  }
  if (status === 'AVAILABLE' || status === 'COMPLETED' || status === 'RETURNED') {
    return 'success'
  }
  return 'info'
}

async function loadStatistics() {
  loading.value = true

  try {
    const payload: StatisticsQuery = {
      rankScope: query.rankScope,
      topN: query.topN
    }

    const [overviewResult, hotResult, damageResult, violationResult] = await Promise.all([
      statisticsOverview(),
      hotDevices(payload),
      deviceDamageStatistics(payload),
      userViolationStatistics(payload)
    ])

    overview.value = overviewResult
    hotDeviceList.value = hotResult
    damageDeviceList.value = damageResult
    violationUserList.value = violationResult
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load statistics')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  void loadStatistics()
}

function handleReset() {
  query.rankScope = 'TOTAL'
  query.topN = 5
  void loadStatistics()
}

onMounted(() => {
  void loadStatistics()
})
</script>

<template>
  <div class="statistics-page" v-loading="loading">
    <section class="hero-card">
      <span class="eyebrow">Statistics Module</span>
      <h2>Overview and ranking dashboards</h2>
      <p>
        This workspace brings the PRD analytics scope into one page: global overview, hot devices, repair pressure,
        and user violation ranking. The time dimension and top list length stay aligned with the backend contract.
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="query.rankScope" placeholder="Rank scope">
          <ElOption v-for="option in scopeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
        <ElSelect v-model="query.topN" placeholder="Top N">
          <ElOption v-for="count in topNOptions" :key="count" :label="`Top ${count}`" :value="count" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Refresh</ElButton>
      </div>
    </section>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.title" class="summary-card">
        <span>{{ card.title }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.description }}</p>
      </article>
    </section>

    <section class="ranking-grid">
      <article class="panel-card">
        <div class="panel-header">
          <div>
            <span class="panel-kicker">Hot Devices</span>
            <h3>Borrowing popularity ranking</h3>
          </div>
          <ElTag>{{ scopeLabel(query.rankScope) }}</ElTag>
        </div>

        <div v-if="hotDeviceList.length" class="ranking-list">
          <div v-for="(item, index) in hotDeviceList" :key="item.deviceId" class="ranking-item">
            <span class="rank-badge">{{ index + 1 }}</span>
            <div class="ranking-main">
              <strong>{{ item.deviceName }}</strong>
              <span>{{ item.deviceCode }}</span>
            </div>
            <div class="metric-box">
              <strong>{{ item.borrowCount }}</strong>
              <span>Borrows</span>
            </div>
          </div>
        </div>
        <ElEmpty v-else description="No hot-device data in the current scope" />
      </article>

      <article class="panel-card">
        <div class="panel-header">
          <div>
            <span class="panel-kicker">Damage Devices</span>
            <h3>Repair pressure ranking</h3>
          </div>
          <ElTag type="warning">{{ scopeLabel(query.rankScope) }}</ElTag>
        </div>

        <div v-if="damageDeviceList.length" class="ranking-list">
          <div v-for="(item, index) in damageDeviceList" :key="item.deviceId" class="ranking-item">
            <span class="rank-badge warning">{{ index + 1 }}</span>
            <div class="ranking-main">
              <strong>{{ item.deviceName }}</strong>
              <span>{{ item.category }} · {{ item.deviceCode }}</span>
            </div>
            <div class="metric-box">
              <strong>{{ item.damageCount }}</strong>
              <span>Repairs</span>
            </div>
            <ElTag size="small" :type="statusTagType(item.status)">{{ item.status }}</ElTag>
          </div>
        </div>
        <ElEmpty v-else description="No damaged-device data in the current scope" />
      </article>

      <article class="panel-card">
        <div class="panel-header">
          <div>
            <span class="panel-kicker">Violation Users</span>
            <h3>Risk user ranking</h3>
          </div>
          <ElTag type="danger">{{ scopeLabel(query.rankScope) }}</ElTag>
        </div>

        <div v-if="violationUserList.length" class="ranking-list">
          <div v-for="(item, index) in violationUserList" :key="item.userId" class="ranking-item">
            <span class="rank-badge danger">{{ index + 1 }}</span>
            <div class="ranking-main">
              <strong>{{ item.name }}</strong>
              <span>{{ item.jobNoOrStudentNo }}</span>
            </div>
            <div class="user-metrics">
              <span>Overdue {{ item.overdueCount }}</span>
              <span>Damage {{ item.damageCount }}</span>
            </div>
            <div class="metric-box">
              <strong>{{ item.violationCount }}</strong>
              <span>Total</span>
            </div>
          </div>
        </div>
        <ElEmpty v-else description="No violation data in the current scope" />
      </article>
    </section>
  </div>
</template>

<style scoped>
.statistics-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.toolbar-card,
.summary-card,
.panel-card {
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

.eyebrow,
.panel-kicker {
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
  grid-template-columns: repeat(2, minmax(0, 220px));
  gap: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: grid;
  gap: 10px;
}

.summary-card span,
.ranking-main span,
.metric-box span,
.user-metrics span,
.summary-card p {
  color: #64748b;
}

.summary-card strong {
  font-size: 28px;
}

.summary-card p {
  margin: 0;
  line-height: 1.6;
}

.ranking-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.panel-card {
  display: grid;
  gap: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.panel-header h3 {
  margin: 10px 0 0;
  font-size: 22px;
}

.ranking-list {
  display: grid;
  gap: 14px;
}

.ranking-item {
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.rank-badge {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  color: #0f172a;
  background: #bae6fd;
}

.rank-badge.warning {
  background: #fde68a;
}

.rank-badge.danger {
  background: #fecaca;
}

.ranking-main {
  display: grid;
  gap: 6px;
}

.metric-box {
  display: grid;
  gap: 4px;
  min-width: 70px;
  text-align: right;
}

.metric-box strong {
  font-size: 24px;
}

.user-metrics {
  display: grid;
  gap: 4px;
  min-width: 88px;
}

@media (max-width: 1200px) {
  .summary-grid,
  .ranking-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .ranking-item {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .metric-box {
    text-align: left;
  }
}
</style>
