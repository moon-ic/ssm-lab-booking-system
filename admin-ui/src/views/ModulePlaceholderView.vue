<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { AppRouteMeta } from '@/router/access'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const authStore = useAuthStore()

const meta = computed(() => ((route.meta ?? {}) as unknown as AppRouteMeta))
const roleCode = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? '--')
</script>

<template>
  <section class="module-card">
    <span class="eyebrow">权限路由</span>
    <h2>{{ meta.title }}</h2>
    <p>该模块已接入权限路由守卫，可根据角色与首次登录状态控制访问。</p>

    <div class="module-grid">
      <article>
        <strong>当前角色</strong>
        <span>{{ roleCode }}</span>
      </article>
      <article>
        <strong>路由路径</strong>
        <span>{{ route.fullPath }}</span>
      </article>
      <article>
        <strong>守卫模式</strong>
        <span>角色元信息 + 首次登录限制</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.module-card {
  display: grid;
  gap: 18px;
  padding: 28px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.module-card h2 {
  margin: 0;
  font-size: 30px;
}

.module-card p {
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

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.module-grid article {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.module-grid span {
  color: #64748b;
}

@media (max-width: 900px) {
  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>


