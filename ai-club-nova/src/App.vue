<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AuroraBackground from './components/AuroraBackground.vue'
import ParticleField from './components/ParticleField.vue'
import PointerGlow from './components/PointerGlow.vue'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'

const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
const isLogin = computed(() => route.path === '/login')
const showChrome = computed(() => !isAdmin.value && !isLogin.value)

const pageKey = computed(() => route.fullPath)
</script>

<template>
  <div class="nx-app" :class="{ 'is-admin': isAdmin, 'is-login': isLogin }">
    <a v-if="!isAdmin" class="nx-skip" href="#main">跳转到主要内容</a>

    <AuroraBackground />
    <ParticleField v-if="!isAdmin" />
    <PointerGlow v-if="!isAdmin" />

    <SiteHeader v-if="showChrome" />

    <main id="main" class="nx-main" :class="{ 'has-chrome': showChrome, 'is-admin': isAdmin }">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="pageKey" />
        </Transition>
      </RouterView>
    </main>

    <SiteFooter v-if="showChrome" />
  </div>
</template>
