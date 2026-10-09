<script setup lang="ts"></script>

<template>
  <header class="app-header">
    <h1>電商購物中心</h1>
    <nav>
      <RouterLink to="/orders/new">建立訂單</RouterLink>
      <RouterLink to="/admin/products">商品管理</RouterLink>
    </nav>
  </header>
  <main class="app-main">
    <RouterView />
  </main>
</template>
