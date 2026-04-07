<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import ForcePasswordDialog from "@/components/ForcePasswordDialog.vue";
import { unconfirmedSummary } from "@/api/messages";
import { findRouteMetaByPath, getAccessibleMenuItems, type AppRouteMeta } from "@/router/access";
import { useAuthStore } from "@/store/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const displayName = computed(
    () => authStore.state.currentUser?.name ?? authStore.state.session?.userInfo.name ?? "未知用户"
);
const roleCode = computed(
    () => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? "--"
);
const profileInitial = computed(() => displayName.value.trim().charAt(0).toUpperCase() || "U");
const firstLoginRequired = computed(() => Boolean(authStore.state.session?.firstLoginRequired));
const menuItems = computed(() => getAccessibleMenuItems(roleCode.value));
const summaryState = ref<{ total: number } | null>(null);
const unreadTotal = computed(() => summaryState.value?.total ?? 0);
const currentMeta = computed<AppRouteMeta | undefined>(() => {
    const matchedMeta = route.meta as unknown as AppRouteMeta | undefined;
    if (matchedMeta?.title) {
        return matchedMeta;
    }
    return findRouteMetaByPath(route.path);
});

async function loadMessageSummary() {
    try {
        const summary = await unconfirmedSummary();
        summaryState.value = summary;
    } catch {
        summaryState.value = null;
    }
}

async function handleLogout() {
    await ElMessageBox.confirm("确认退出登录并返回登录页吗？", "退出登录", {
        confirmButtonText: "退出登录",
        cancelButtonText: "取消",
        type: "warning"
    });

    authStore.signOut();
    await router.replace("/login");
}

async function goToMessages() {
    await router.push("/messages");
}

async function goToProfile() {
    if (route.path === "/profile") {
        return;
    }

    await router.push("/profile");
}

onMounted(() => {
    void loadMessageSummary();
});

watch(
    () => route.fullPath,
    () => {
        void loadMessageSummary();
    }
);
</script>

<template>
    <div class="layout-shell">
        <aside class="layout-sidebar">
            <div class="brand">
                <strong>实验室后台</strong>
                <span>菜单已按角色固定配置</span>
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
                </RouterLink>
            </nav>
        </aside>

        <main class="layout-main">
            <header class="layout-header">
                <div class="module-copy">
                    <span class="module-eyebrow">{{ currentMeta?.title ?? "系统导航" }}</span>
                </div>

                <div class="header-actions">
                    <button class="message-button" type="button" aria-label="消息中心" @click="goToMessages">
                        <svg viewBox="0 0 24 24" class="message-icon" aria-hidden="true">
                            <path
                                d="M12 3a6 6 0 0 0-6 6v3.3c0 .7-.28 1.37-.78 1.87L4 15.4V17h16v-1.6l-1.22-1.2a2.65 2.65 0 0 1-.78-1.87V9a6 6 0 0 0-6-6Zm0 19a3 3 0 0 0 2.82-2H9.18A3 3 0 0 0 12 22Z"
                                fill="currentColor"
                            />
                        </svg>
                        <span v-if="unreadTotal > 0" class="message-dot" />
                    </button>

                    <button class="profile-entry" type="button" aria-label="进入个人中心" @click="goToProfile">
                        <span class="avatar-chip" aria-hidden="true">{{ profileInitial }}</span>
                        <span class="user-card">
                            <strong>{{ displayName }}</strong>
                            <span>{{ roleCode }}</span>
                        </span>
                    </button>

                    <button class="logout-button" type="button" @click="handleLogout">退出登录</button>
                </div>
            </header>

            <section v-if="firstLoginRequired" class="first-login-banner">
                当前仍处于首次登录状态，修改密码前仅可访问首页与个人中心。
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
    color: #f8fbff;
    background:
        radial-gradient(circle at top, rgba(224, 233, 246, 0.34), transparent 24%),
        linear-gradient(180deg, #4b83cd 0%, #91b3e0 100%);
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
    color: rgba(248, 251, 255, 0.82);
    font-size: 13px;
}

.nav-list {
    display: grid;
    gap: 10px;
}

.nav-item {
    display: block;
    padding: 14px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.12);
    transition:
        transform 0.18s ease,
        background 0.18s ease;
}

.nav-item:hover,
.nav-item.is-active {
    transform: translateX(2px);
    background: rgba(255, 255, 255, 0.24);
}

.layout-main {
    padding: 28px;
}

.layout-header {
    display: flex;
    justify-content: space-between;
    gap: 20px;
    align-items: center;
    margin-bottom: 20px;
    padding: 18px 20px;
    border-radius: 24px;
    color: #f8fbff;
    background: linear-gradient(135deg, rgba(75, 131, 205, 0.98), rgba(145, 179, 224, 0.94));
    box-shadow: 0 18px 40px rgba(75, 131, 205, 0.2);
}

.module-copy {
    display: grid;
    gap: 8px;
}

.module-eyebrow {
    display: inline-flex;
    width: fit-content;
    padding: 6px 10px;
    border-radius: 999px;
    color: #1d4f8f;
    background: rgba(255, 255, 255, 0.82);
    font-size: 16px;
    font-weight: 700;
    letter-spacing: 0.08em;
}

.module-copy p {
    margin: 0;
    color: rgba(248, 251, 255, 0.9);
    line-height: 1.7;
}

.header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
}

.message-button,
.profile-entry,
.logout-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 0;
    cursor: pointer;
}

.message-button {
    position: relative;
    width: 48px;
    height: 48px;
    border-radius: 16px;
    color: var(--theme-primary);
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 14px 30px rgba(43, 76, 120, 0.12);
}

.message-icon {
    width: 22px;
    height: 22px;
}

.message-dot {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 10px;
    height: 10px;
    border-radius: 999px;
    background: #dc2626;
    box-shadow: 0 0 0 2px #fff;
}

.profile-entry {
    gap: 12px;
    padding: 8px 10px 8px 8px;
    border-radius: 20px;
    color: inherit;
    background: rgba(255, 255, 255, 0.12);
    box-shadow: 0 14px 30px rgba(43, 76, 120, 0.1);
}

.avatar-chip {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 16px;
    color: #1d4f8f;
    background: rgba(255, 255, 255, 0.92);
    font-size: 18px;
    font-weight: 800;
}

.user-card {
    display: grid;
    gap: 4px;
    min-width: 148px;
    text-align: left;
}

.user-card span {
    color: rgba(248, 251, 255, 0.82);
    font-size: 13px;
}

.logout-button {
    padding: 12px 16px;
    border-radius: 16px;
    color: #f8fbff;
    background: rgba(29, 79, 143, 0.96);
}

.first-login-banner {
    margin-bottom: 18px;
    padding: 14px 16px;
    border: 1px solid rgba(75, 131, 205, 0.22);
    border-radius: 16px;
    color: #295f9f;
    background: rgba(224, 233, 246, 0.78);
}

.layout-content {
    min-height: 400px;
}

@media (max-width: 980px) {
    .layout-shell {
        grid-template-columns: 1fr;
    }

    .layout-header,
    .header-actions {
        flex-direction: column;
        align-items: flex-start;
    }

    .header-actions {
        width: 100%;
    }
}
</style>
