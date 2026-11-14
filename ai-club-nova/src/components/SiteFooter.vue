<script setup lang="ts">
function toTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <footer class="nx-footer">
    <div class="nx-footer__glow" aria-hidden="true"></div>
    <div class="nx-footer__inner">
      <div class="nx-footer__brand">
        <RouterLink class="nx-footer__logo" to="/">
          <span class="nx-footer__logo-mark">AI<span>✳</span></span>
          <span class="nx-footer__logo-text">人工智能社<small>ARTIFICIAL INTELLIGENCE CLUB</small></span>
        </RouterLink>
        <p>在这里，让好奇发生。</p>
        <p class="nx-footer__tags">人工智能 · 竞赛实践 · 科研探索</p>
      </div>

      <div class="nx-footer__col">
        <b>一起探索</b>
        <RouterLink to="/projects">项目与成果</RouterLink>
        <RouterLink to="/news">新闻与活动</RouterLink>
        <RouterLink to="/join">加入我们</RouterLink>
      </div>

      <div class="nx-footer__col">
        <b>保持联系</b>
        <p>联系方式：待补充</p>
        <RouterLink to="/login">管理员登录 ↗</RouterLink>
        <button class="nx-footer__top" @click="toTop">回到顶部 ↑</button>
      </div>
    </div>

    <div class="nx-footer__word" aria-hidden="true">AI CLUB</div>

    <div class="nx-footer__bottom">
      <span>© {{ new Date().getFullYear() }} 人工智能社</span>
      <span>保持好奇，持续创造。</span>
    </div>
  </footer>
</template>
