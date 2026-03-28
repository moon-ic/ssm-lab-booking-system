<template>
  <div class="dashboard-grid">
    <section class="hero-card">
      <span class="eyebrow">Project Bootstrap</span>
      <h2>项目已经完成前后端基础初始化</h2>
      <p>
        当前页面用于确认路由、布局和样式链路正常，后续可以继续接入登录、设备管理、预约审核和统计分析模块。
      </p>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <strong>admin-ui</strong>
        <span>Vue 3 + TypeScript + Vite</span>
      </article>
      <article class="stat-card">
        <strong>admin-server</strong>
        <span>Spring Boot 3 + Security + MyBatis Plus</span>
      </article>
      <article class="stat-card">
        <strong>API Prefix</strong>
        <span>/api</span>
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

