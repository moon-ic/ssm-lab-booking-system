<script setup lang="ts">
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ForcePasswordDialog from '@/components/ForcePasswordDialog.vue'
import { getAccessibleMenuItems } from '@/router/access'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(() => authStore.state.currentUser?.name ?? authStore.state.session?.userInfo.name ?? 'Unknown user')
const roleCode = computed(() => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? '--')
const firstLoginRequired = computed(() => Boolean(authStore.state.session?.firstLoginRequired))
const menuItems = computed(() => getAccessibleMenuItems(roleCode.value))

async function handleLogout() {
  await ElMessageBox.confirm('Sign out and return to the login page?', 'Sign out', {
    confirmButtonText: 'Sign out',
    cancelButtonText: 'Cancel',
    type: 'warning'
  })

  authStore.signOut()
  await router.replace('/login')
}
</script>

<template>
  <div class="layout-shell">
    <aside class="layout-sidebar">
      <div class="brand">
        <strong>Lab Admin</strong>
        <span>Role-based navigation</span>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ 'is-active': route.path === item.path }"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.description }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="layout-main">
      <header class="layout-header">
        <div class="header-copy">
          <h1>Permission Routing</h1>
          <p>
            Menus are filtered by role. Routes are protected by auth, role access, and first-login password rules.
          </p>
        </div>

        <div class="header-user">
          <div class="user-card">
            <strong>{{ displayName }}</strong>
            <span>{{ roleCode }}</span>
          </div>
          <button class="logout-button" type="button" @click="handleLogout">Sign out</button>
        </div>
      </header>

      <section v-if="firstLoginRequired" class="first-login-banner">
        First login is still pending. Only the dashboard stays available until the password is changed.
      </section>

      <section class="layout-content">
        <RouterView />
      </section>
    </main>

    <ForcePasswordDialog />
  </div>
</template>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 100vh;
}

.layout-sidebar {
  padding: 24px 18px;
  color: #f8fafc;
  background:
    radial-gradient(circle at top, rgba(250, 204, 21, 0.2), transparent 24%),
    linear-gradient(180deg, #0f172a 0%, #155e75 100%);
}

.brand {
  display: grid;
  gap: 6px;
  margin-bottom: 28px;
}

.brand strong {
  font-size: 24px;
  letter-spacing: 0.04em;
}

.brand span {
  color: rgba(226, 232, 240, 0.78);
  font-size: 13px;
}

.nav-list {
  display: grid;
  gap: 10px;
}

.nav-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  transition: transform 0.18s ease, background 0.18s ease;
}

.nav-item:hover,
.nav-item.is-active {
  transform: translateX(2px);
  background: rgba(255, 255, 255, 0.18);
}

.nav-item span {
  color: rgba(226, 232, 240, 0.78);
  font-size: 12px;
  line-height: 1.5;
}

.layout-main {
  padding: 28px;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.layout-header h1 {
  margin: 0;
  font-size: 32px;
}

.layout-header p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.7;
}

.header-user {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-card {
  display: grid;
  gap: 4px;
  min-width: 148px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
}

.user-card span {
  color: #64748b;
  font-size: 13px;
}

.logout-button {
  padding: 12px 16px;
  border: 0;
  border-radius: 16px;
  color: #f8fafc;
  cursor: pointer;
  background: #0f766e;
}

.first-login-banner {
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: 16px;
  color: #92400e;
  background: #fef3c7;
}

.layout-content {
  min-height: 400px;
}

@media (max-width: 980px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .layout-header,
  .header-user {
    flex-direction: column;
  }
}
</style>
