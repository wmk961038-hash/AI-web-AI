<template>
  <section class="nx-notfound">
    <div class="nx-notfound__grid" aria-hidden="true"></div>
    <div class="nx-notfound__inner">
      <p class="nx-kicker">SIGNAL LOST / 404</p>
      <h1>这个页面<br /><span>还没有发生。</span></h1>
      <p class="nx-notfound__desc">链接可能已失效，或内容尚未发布。</p>
      <div class="nx-notfound__actions">
        <RouterLink class="nx-btn nx-btn--primary" to="/">
          <span>返回首页</span><i aria-hidden="true">→</i>
        </RouterLink>
        <RouterLink class="nx-btn nx-btn--ghost" to="/projects">
          <span>看看我们的项目</span><i aria-hidden="true">↗</i>
        </RouterLink>
      </div>
      <div class="nx-notfound__code" aria-hidden="true">404</div>
    </div>
  </section>
</template>
