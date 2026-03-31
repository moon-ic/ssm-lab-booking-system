<template>
  <section class="forbidden-card">
    <span class="eyebrow">403</span>
    <h2>Access denied</h2>
    <p>
      Your current role does not have permission to open this module. The route guard blocked access and kept the
      rest of the app state intact.
    </p>
    <RouterLink class="back-link" to="/">Back to dashboard</RouterLink>
  </section>
</template>

<style scoped>
.forbidden-card {
  display: grid;
  gap: 16px;
  padding: 36px;
  border: 1px solid rgba(248, 113, 113, 0.24);
  border-radius: 24px;
  background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.forbidden-card h2 {
  margin: 0;
  font-size: 32px;
}

.forbidden-card p {
  margin: 0;
  line-height: 1.7;
  color: #7c2d12;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: #991b1b;
  background: #fee2e2;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.back-link {
  width: fit-content;
  padding: 12px 16px;
  border-radius: 14px;
  color: #fff7ed;
  background: #9a3412;
}
</style>
