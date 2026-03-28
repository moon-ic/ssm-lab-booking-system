<template>
  <div class="layout-shell">
    <aside class="layout-sidebar">
      <div class="brand">Lab Admin</div>
      <nav class="nav-list">
        <div class="nav-item is-active">首页概览</div>
        <div class="nav-item">设备管理</div>
        <div class="nav-item">预约审核</div>
        <div class="nav-item">借用记录</div>
      </nav>
    </aside>
    <main class="layout-main">
      <header class="layout-header">
        <div>
          <h1>实验室设备管理系统</h1>
          <p>前端基础骨架已初始化，可继续接入登录、权限和业务模块。</p>
        </div>
      </header>
      <section class="layout-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
}

.layout-sidebar {
  padding: 24px 18px;
  color: #f8fafc;
  background: linear-gradient(180deg, #16324f 0%, #0e7490 100%);
}

.brand {
  margin-bottom: 28px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.nav-list {
  display: grid;
  gap: 10px;
}

.nav-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
}

.nav-item.is-active {
  background: rgba(255, 255, 255, 0.18);
}

.layout-main {
  padding: 28px;
}

.layout-header {
  margin-bottom: 20px;
}

.layout-header h1 {
  margin: 0;
  font-size: 32px;
}

.layout-header p {
  margin: 8px 0 0;
  color: #475569;
}

.layout-content {
  min-height: 400px;
}

@media (max-width: 900px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .layout-sidebar {
    padding-bottom: 12px;
  }
}
</style>

