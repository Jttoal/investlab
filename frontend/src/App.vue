<template>
  <div id="app">
    <nav class="navbar">
      <div class="navbar__container">
        <h1 class="navbar__title">InvestLab</h1>
        <ul class="navbar__menu">
          <li><router-link to="/">仪表盘</router-link></li>
          <li><router-link to="/strategies">策略管理</router-link></li>
          <li><router-link to="/accounts">账户管理</router-link></li>
          <li><router-link to="/household-bills">家庭账单</router-link></li>
          <li><router-link to="/settings">系统设置</router-link></li>
        </ul>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
</script>

<style scoped>
.navbar {
  background-color: #2c3e50;
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.navbar__container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navbar__title {
  margin: 0;
  font-size: 1.5rem;
}

.navbar__menu {
  list-style: none;
  display: flex;
  gap: 2rem;
  margin: 0;
  padding: 0;
}

.navbar__menu a {
  color: white;
  text-decoration: none;
  transition: opacity 0.2s;
}

.navbar__menu a:hover,
.navbar__menu a.router-link-active {
  opacity: 0.8;
  border-bottom: 2px solid white;
}

.main-content {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 1rem;
}
</style>
