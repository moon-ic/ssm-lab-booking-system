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
    <span class="eyebrow">Permission Routed</span>
    <h2>{{ meta.title }}</h2>
    <p>{{ meta.description }}</p>

    <div class="module-grid">
      <article>
        <strong>Current role</strong>
        <span>{{ roleCode }}</span>
      </article>
      <article>
        <strong>Route path</strong>
        <span>{{ route.fullPath }}</span>
      </article>
      <article>
        <strong>Guard mode</strong>
        <span>meta.roles + first-login restriction</span>
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
  color: #155e75;
  background: #cffafe;
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
