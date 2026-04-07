<script setup lang="ts">
import { computed } from 'vue'
import { getAccessibleMenuItems } from '@/router/access'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()

const userName = computed(() => authStore.state.currentUser?.name ?? authStore.state.session?.userInfo.name ?? '--')
const roleCode = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? '--')
const firstLoginRequired = computed(() => Boolean(authStore.state.session?.firstLoginRequired))
const menuItems = computed(() => getAccessibleMenuItems(roleCode.value))
</script>

<template>
  <div class="dashboard-grid">
    <section class="hero-card">
      <span class="eyebrow">权限就绪</span>
      <h2>已启用按角色控制的路由访问</h2>
      <p>
        当前登录用户为 <strong>{{ userName }}</strong>，角色为 <strong>{{ roleCode }}</strong>。侧边菜单和页面访问权限
        已按 PRD 与接口约定生效。
      </p>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <strong>首次登录状态</strong>
        <span>{{ firstLoginRequired ? '修改密码前仅可访问首页' : '已解锁全部可访问模块' }}</span>
      </article>
      <article class="stat-card">
        <strong>可见菜单</strong>
        <span>{{ menuItems.length }} 项</span>
      </article>
      <article class="stat-card">
        <strong>权限策略</strong>
        <span>登录校验 + 角色元信息 + 403 兜底</span>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  gap: 20px;
}

.hero-card,
.stat-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.hero-card {
  padding: 28px;
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
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: grid;
  gap: 10px;
  padding: 22px;
}

.stat-card strong {
  font-size: 18px;
}

.stat-card span {
  color: #64748b;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>

