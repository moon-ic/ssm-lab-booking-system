<template>
  <section class="forbidden-card">
    <span class="eyebrow">403</span>
    <h2>无权访问</h2>
    <p>
      当前角色没有进入该模块的权限。路由守卫已拦截本次访问，同时保留了应用其他状态不受影响。
    </p>
    <RouterLink class="back-link" to="/">返回首页</RouterLink>
  </section>
</template>

<style scoped>
.forbidden-card {
  display: grid;
  gap: 16px;
  padding: 36px;
  border: 1px solid rgba(75, 131, 205, 0.22);
  border-radius: 24px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  box-shadow: 0 20px 60px rgba(75, 131, 205, 0.12);
}

.forbidden-card h2 {
  margin: 0;
  font-size: 32px;
}

.forbidden-card p {
  margin: 0;
  line-height: 1.7;
  color: var(--theme-text-soft);
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

.back-link {
  width: fit-content;
  padding: 12px 16px;
  border-radius: 14px;
  color: #f8fbff;
  background: var(--theme-primary);
}
</style>
