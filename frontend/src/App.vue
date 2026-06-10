<template>
  <NavBar />
  <div class="main-container">
    <router-view />
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Microsoft YaHei', sans-serif; background: #f5f7fa; }
.main-container { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
</style>
