<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/store/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const formRef = ref();
const loading = ref(false);

const form = reactive({
    loginId: "",
    password: ""
});

const rules = {
    loginId: [{ required: true, message: "请输入账号", trigger: "blur" }],
    password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

async function submit() {
    await formRef.value?.validate();
    loading.value = true;

    try {
        const session = await authStore.signIn(form);
        const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";

        if (session.firstLoginRequired) {
            ElMessage.warning("首次登录请先修改密码");
        } else {
            ElMessage.success(`欢迎回来，${session.userInfo.name}`);
        }

        await router.replace(redirect);
    } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : "登录失败");
    } finally {
        loading.value = false;
    }
}

function fillDemo(loginId: string, password: string) {
    form.loginId = loginId;
    form.password = password;
}
</script>

<template>
    <div class="login-page">
        <section class="login-hero">
            <span class="login-badge">设备借用管理后台</span>
            <h1>实验设备借用后台</h1>
            <p>
                登录后可继续进入设备管理、预约审核、借用记录和后续业务模块。当前页面已内置 mock
                场景，便于前后端并行开发。
            </p>
            <div class="demo-panel">
                <button type="button" @click="fillDemo('SA001', '0000')">超级管理员：SA001 / 00000000</button>
                <button type="button" @click="fillDemo('A001', '0000')">管理员：A001 / 00000000</button>
                <button type="button" @click="fillDemo('T2026001', '00000000')">教师：T2026001 / 00000000</button>
                <button type="button" @click="fillDemo('20230001', '00000000')">学生：20230001 / 00000000</button>
            </div>
        </section>

        <section class="login-card">
            <div class="card-header">
                <h2>账号登录</h2>
                <p>支持账号状态校验、首次登录改密提醒和登录态持久化。</p>
            </div>

            <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
                <ElFormItem label="登录账号" prop="loginId">
                    <ElInput v-model="form.loginId" placeholder="请输入工号、学号或账号" size="large" />
                </ElFormItem>
                <ElFormItem label="登录密码" prop="password">
                    <ElInput
                        v-model="form.password"
                        type="password"
                        placeholder="请输入密码"
                        size="large"
                        show-password
                        @keyup.enter="submit"
                    />
                </ElFormItem>

                <ElButton class="submit-button" type="primary" size="large" :loading="loading" @click="submit">
                    登录系统
                </ElButton>
            </ElForm>

            <ul class="login-tips">
                <li>当前联调用演示账号密码为 `00000000`。</li>
                <li>如果更新了环境配置或后端权限规则，记得重启 Vite 前端服务。</li>
            </ul>
        </section>
    </div>
</template>

<style scoped>
.login-page {
    display: grid;
    grid-template-columns: minmax(0, 1.1fr) minmax(360px, 420px);
    min-height: 100vh;
    padding: 32px;
    gap: 24px;
}

.login-hero,
.login-card {
    position: relative;
    overflow: hidden;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 28px;
    box-shadow: 0 28px 80px rgba(15, 23, 42, 0.12);
}

.login-hero {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 640px;
    padding: 56px;
    color: #f8fbff;
    background:
        radial-gradient(circle at top right, rgba(255, 255, 255, 0.28), transparent 30%),
        radial-gradient(circle at bottom left, rgba(224, 233, 246, 0.3), transparent 28%),
        linear-gradient(140deg, #4b83cd 0%, #91b3e0 100%);
}

.login-badge {
    display: inline-flex;
    width: fit-content;
    padding: 8px 14px;
    border-radius: 999px;
    border: 1px solid rgba(255, 255, 255, 0.22);
    background: rgba(255, 255, 255, 0.16);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
}

.login-hero h1 {
    margin: 18px 0 16px;
    font-size: clamp(38px, 5vw, 64px);
    line-height: 1.05;
}

.login-hero p {
    max-width: 560px;
    margin: 0;
    font-size: 17px;
    line-height: 1.8;
    color: rgba(248, 251, 255, 0.9);
}

.demo-panel {
    display: grid;
    gap: 12px;
    margin-top: 32px;
}

.demo-panel button {
    width: 100%;
    padding: 14px 16px;
    border: 0;
    border-radius: 18px;
    color: inherit;
    text-align: left;
    font: inherit;
    cursor: pointer;
    background: rgba(255, 255, 255, 0.14);
    transition:
        transform 0.2s ease,
        background 0.2s ease;
}

.demo-panel button:hover {
    transform: translateY(-1px);
    background: rgba(255, 255, 255, 0.22);
}

.login-card {
    align-self: center;
    padding: 36px 32px;
    border-color: rgba(75, 131, 205, 0.18);
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(18px);
}

.card-header h2 {
    margin: 0;
    font-size: 30px;
    color: var(--theme-text);
}

.card-header p {
    margin: 10px 0 24px;
    line-height: 1.7;
    color: var(--theme-text-soft);
}

.submit-button {
    width: 100%;
    margin-top: 8px;
}

.login-tips {
    margin: 24px 0 0;
    padding-left: 18px;
    color: var(--theme-text-soft);
    line-height: 1.8;
}

@media (max-width: 980px) {
    .login-page {
        grid-template-columns: 1fr;
        padding: 18px;
    }

    .login-hero {
        min-height: auto;
        padding: 32px 24px;
    }

    .login-card {
        padding: 28px 22px;
    }
}
</style>
