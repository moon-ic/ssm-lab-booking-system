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
  { label: '累计', value: 'TOTAL' },
  { label: '近半年', value: 'HALF_YEAR' },
  { label: '近一个月', value: 'MONTH' }
]

const topNOptions = [3, 5, 10]

const summaryCards = computed(() => {
  if (!overview.value) {
    return []
  }

  return [
    {
      title: '系统设备数',
      value: overview.value.deviceTotal,
      description: '当前工作台内已登记的设备总数。'
    },
    {
      title: '当前可用',
      value: overview.value.availableDeviceTotal,
      description: '当前可预约或可借用的设备数量。'
    },
    {
      title: '借用中',
      value: overview.value.borrowingTotal,
      description: '处于借用中或逾期中的记录数。'
    },
    {
      title: '待审核预约',
      value: overview.value.pendingReservationTotal,
      description: '仍在等待审核的预约申请数量。'
    },
    {
      title: '待处理报修',
      value: overview.value.pendingRepairTotal,
      description: '仍需处理的维修申请数量。'
    }
  ]
})

function scopeLabel(scope: RankScope) {
  const option = scopeOptions.find((item) => item.value === scope)
  return option?.label ?? scope
}

function statisticsStatusLabel(status: string) {
  switch (status) {
    case 'AVAILABLE':
      return '可用'
    case 'COMPLETED':
      return '已完成'
    case 'RETURNED':
      return '已归还'
    case 'REPAIRING':
      return '维修中'
    case 'PROCESSING':
      return '处理中'
    case 'DAMAGED':
      return '已损坏'
    case 'UNREPAIRABLE':
      return '无法修复'
    case 'OVERDUE':
      return '已逾期'
    default:
      return status
  }
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
    ElMessage.error(error instanceof Error ? error.message : '加载统计数据失败')
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
      <span class="eyebrow">统计模块</span>
      <h2>概览与排行看板</h2>
      <p>
        当前页面将 PRD 中的分析范围集中到一个工作台，包含整体概览、热门设备、维修压力和用户违规排行，
        时间维度与 Top N 设置均与后端约定保持一致。
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElSelect v-model="query.rankScope" placeholder="统计范围">
          <ElOption v-for="option in scopeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
        <ElSelect v-model="query.topN" placeholder="排行数量">
          <ElOption v-for="count in topNOptions" :key="count" :label="`前 ${count}`" :value="count" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">重置</ElButton>
        <ElButton type="primary" @click="handleSearch">刷新</ElButton>
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
            <span class="panel-kicker">热门设备</span>
            <h3>借用热度排行</h3>
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
              <span>借用次数</span>
            </div>
          </div>
        </div>
        <ElEmpty v-else description="当前范围暂无热门设备数据" />
      </article>

      <article class="panel-card">
        <div class="panel-header">
          <div>
            <span class="panel-kicker">故障设备</span>
            <h3>维修压力排行</h3>
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
              <span>报修次数</span>
            </div>
            <ElTag size="small" :type="statusTagType(item.status)">{{ statisticsStatusLabel(item.status) }}</ElTag>
          </div>
        </div>
        <ElEmpty v-else description="当前范围暂无故障设备数据" />
      </article>

      <article class="panel-card">
        <div class="panel-header">
          <div>
            <span class="panel-kicker">违规用户</span>
            <h3>风险用户排行</h3>
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
              <span>逾期 {{ item.overdueCount }}</span>
              <span>损坏 {{ item.damageCount }}</span>
            </div>
            <div class="metric-box">
              <strong>{{ item.violationCount }}</strong>
              <span>总计</span>
            </div>
          </div>
        </div>
        <ElEmpty v-else description="当前范围暂无违规数据" />
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
