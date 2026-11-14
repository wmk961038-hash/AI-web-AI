<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute(),
  open = ref(false)
watch(
  () => route.fullPath,
  () => (open.value = false),
)
</script>
<template>
  <a class="skip" href="#main">跳转到主要内容</a>
  <header class="site-header">
    <div class="nav-wrap">
      <RouterLink class="brand" to="/" aria-label="人工智能社首页"
        ><span class="brand-icon">AI<span>✳</span></span
        ><span>人工智能社<small>ARTIFICIAL INTELLIGENCE CLUB</small></span></RouterLink
      ><button
        class="menu-toggle"
        @click="open = !open"
        :aria-expanded="open"
        aria-controls="navigation"
      >
        {{ open ? '关闭' : '菜单' }} ☰
      </button>
      <nav id="navigation" :class="{ open }">
        <RouterLink to="/" exact-active-class="active">首页</RouterLink
        ><RouterLink to="/about">关于我们</RouterLink><RouterLink to="/news">新闻与活动</RouterLink
        ><RouterLink to="/projects">项目与成果</RouterLink
        ><RouterLink to="/people">师生风采</RouterLink
        ><RouterLink class="join-link" to="/join">加入我们 ↗</RouterLink>
      </nav>
    </div>
  </header>
  <main id="main"><RouterView :key="route.fullPath" /></main>
  <footer>
    <div class="footer-grid">
      <div>
        <RouterLink class="footer-brand" to="/">AI CLUB <span>✳</span></RouterLink>
        <p>在这里，让好奇发生。</p>
        <small>人工智能 · 竞赛实践 · 科研探索</small>
      </div>
      <div>
        <b>一起探索</b><RouterLink to="/projects">项目与成果</RouterLink
        ><RouterLink to="/news">新闻与活动</RouterLink><RouterLink to="/join">加入我们</RouterLink>
      </div>
      <div>
        <b>保持联系</b>
        <p>联系方式：待补充</p>
        <RouterLink to="/login">管理员登录 ↗</RouterLink>
      </div>
    </div>
    <div class="footer-bottom">
      © {{ new Date().getFullYear() }} 人工智能社 <span>保持好奇，持续创造。</span>
    </div>
  </footer>
</template>
