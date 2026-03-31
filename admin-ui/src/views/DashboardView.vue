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
      <span class="eyebrow">Permission Ready</span>
      <h2>Role-aware routing is now active</h2>
      <p>
        Signed in as <strong>{{ userName }}</strong> with role <strong>{{ roleCode }}</strong>. The sidebar and route
        access now follow the role definition from the PRD and API contract.
      </p>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <strong>First-login mode</strong>
        <span>{{ firstLoginRequired ? 'Dashboard only until password change' : 'All eligible modules unlocked' }}</span>
      </article>
      <article class="stat-card">
        <strong>Visible menus</strong>
        <span>{{ menuItems.length }} items</span>
      </article>
      <article class="stat-card">
        <strong>Guard strategy</strong>
        <span>auth + role meta + 403 fallback</span>
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
  color: #155e75;
  background: #cffafe;
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
