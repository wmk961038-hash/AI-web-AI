<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const open = ref(false)
const scrolled = ref(false)
const progress = ref(0)

const links = [
  { to: '/', label: '首页', en: 'HOME' },
  { to: '/about', label: '关于我们', en: 'ABOUT' },
  { to: '/news', label: '新闻与活动', en: 'NEWS' },
  { to: '/projects', label: '项目与成果', en: 'PROJECTS' },
  { to: '/people', label: '师生风采', en: 'PEOPLE' },
  { to: '/join', label: '加入我们', en: 'JOIN' },
]

function isActive(path: string) {
  return path === '/' ? route.path === '/' : route.path.startsWith(path)
}

function onScroll() {
  const y = window.scrollY
  scrolled.value = y > 24
  const max = document.documentElement.scrollHeight - window.innerHeight
  progress.value = max > 0 ? Math.min(1, y / max) : 0
}

function onKey(event: KeyboardEvent) {
  if (event.key === 'Escape') open.value = false
}

watch(
  () => route.fullPath,
  () => {
    open.value = false
  },
)

watch(open, (value) => {
  document.documentElement.style.overflow = value ? 'hidden' : ''
})

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll)
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
  window.removeEventListener('keydown', onKey)
  document.documentElement.style.overflow = ''
})

const progressStyle = computed(() => ({ transform: `scaleX(${progress.value})` }))
</script>

<template>
  <header class="nx-header" :class="{ 'is-scrolled': scrolled, 'is-open': open }">
    <div class="nx-header__inner">
      <RouterLink class="nx-brand" to="/" aria-label="人工智能社首页">
        <img
          class="nx-brand__mark"
          src="/brand/club-mark.png"
          alt=""
          width="52"
          height="44"
          decoding="async"
        />
        <span class="nx-brand__text">
          <strong>人工智能社</strong>
          <small>ARTIFICIAL INTELLIGENCE CLUB</small>
        </span>
      </RouterLink>

      <nav class="nx-nav" aria-label="主导航">
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          class="nx-nav__link"
          :class="{ active: isActive(link.to) }"
        >
          <span>{{ link.label }}</span>
          <i></i>
        </RouterLink>
      </nav>

      <div class="nx-header__actions">
        <RouterLink class="nx-header__admin" to="/login" title="管理员登录">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 3l7 3v5c0 4.4-2.9 8.3-7 10-4.1-1.7-7-5.6-7-10V6l7-3Z"
              fill="none"
              stroke="currentColor"
              stroke-width="1.4"
            />
            <path
              d="M9.4 11.6a2.6 2.6 0 1 0 5.2 0 2.6 2.6 0 0 0-5.2 0Zm.4 5.3c.7-.8 1.4-1.2 2.2-1.2s1.5.4 2.2 1.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.4"
              stroke-linecap="round"
            />
          </svg>
          <span>ADMIN</span>
        </RouterLink>
        <button
          class="nx-menu-btn"
          :class="{ active: open }"
          :aria-expanded="open"
          aria-controls="nx-mobile-navigation"
          aria-label="打开菜单"
          @click="open = !open"
        >
          <span></span><span></span>
        </button>
      </div>
    </div>
    <div class="nx-scroll-progress" :style="progressStyle"></div>
  </header>

  <Teleport to="body">
    <Transition name="menu">
      <div v-if="open" id="nx-mobile-navigation" class="nx-mobile">
        <div class="nx-mobile__bg" aria-hidden="true"></div>
        <div class="nx-mobile__inner">
          <p class="nx-kicker">NAVIGATION / 目录</p>
          <nav aria-label="移动端导航">
            <RouterLink
              v-for="(link, index) in links"
              :key="link.to"
              :to="link.to"
              class="nx-mobile__link"
              :class="{ active: isActive(link.to) }"
              :style="{ '--i': index }"
            >
              <small>0{{ index + 1 }}</small>
              <span>{{ link.label }}</span>
              <em>{{ link.en }}</em>
            </RouterLink>
          </nav>
          <div class="nx-mobile__foot">
            <RouterLink to="/login">管理员登录 ↗</RouterLink>
            <span>AI CLUB · {{ new Date().getFullYear() }}</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
