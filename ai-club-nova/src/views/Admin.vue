<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, message, refreshCsrf } from '../api'
import { useAuth } from '../stores/auth'
import AdminAccounts from '../components/AdminAccounts.vue'
import AdminAudit from '../components/AdminAudit.vue'
import AdminSections from '../components/AdminSections.vue'
import BrandMark from '../components/BrandMark.vue'

const auth = useAuth()
const router = useRouter()
const tab = ref('content')
const navOpen = ref(false)
const contentTotal = ref(0)
const busy = ref(false)
const passwordDialog = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '' })

const tabTitles: Record<string, { title: string; eyebrow: string }> = {
  content: { title: '内容管理', eyebrow: 'CONTENT OVERVIEW' },
  audit: { title: '操作记录', eyebrow: 'ACTIVITY LOG' },
  accounts: { title: '账号管理', eyebrow: 'ACCOUNTS & ROLES' },
}

function selectTab(name: string) {
  tab.value = name
  navOpen.value = false
}

async function logout() {
  try {
    await auth.logout()
    await router.push('/login')
  } catch (e) {
    ElMessage.error(message(e))
  }
}

async function changePassword() {
  busy.value = true
  try {
    await api.post('/auth/password', passwordForm.value)
    auth.user = null
    await refreshCsrf()
    ElMessage.success('密码已修改，请重新登录')
    await router.push('/login')
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="nx-admin">
    <aside class="nx-admin__sidebar" :class="{ open: navOpen }">
      <RouterLink class="nx-admin__brand" to="/" aria-label="人工智能社首页">
        <BrandMark />
        <span>人工智能社<small>CONTENT STUDIO</small></span>
      </RouterLink>

      <div class="nx-admin__user">
        <span class="nx-admin__avatar">{{ auth.user?.username?.slice(0, 1).toUpperCase() }}</span>
        <div>
          <b>{{ auth.user?.username }}</b>
          <small>{{ auth.user?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}</small>
        </div>
        <i class="nx-admin__online" title="在线"></i>
      </div>

      <nav class="nx-admin__nav" aria-label="后台导航">
        <p class="nx-admin__nav-label">WORKSPACE</p>
        <button :class="{ active: tab === 'content' }" @click="selectTab('content')">
          <span class="nx-admin__nav-icon">▤</span>
          <span>内容管理</span>
          <small>{{ contentTotal }}</small>
        </button>
        <button :class="{ active: tab === 'audit' }" @click="selectTab('audit')">
          <span class="nx-admin__nav-icon">◷</span>
          <span>操作记录</span>
        </button>
        <button
          v-if="auth.user?.role === 'SUPER_ADMIN'"
          :class="{ active: tab === 'accounts' }"
          @click="selectTab('accounts')"
        >
          <span class="nx-admin__nav-icon">♙</span>
          <span>账号管理</span>
        </button>

        <p class="nx-admin__nav-label nx-admin__nav-label--spaced">SYSTEM</p>
        <button @click="passwordDialog = true">
          <span class="nx-admin__nav-icon">⚙</span>
          <span>修改密码</span>
        </button>
        <RouterLink class="nx-admin__nav-link" to="/">
          <span class="nx-admin__nav-icon">◈</span>
          <span>浏览官网</span>
          <small>↗</small>
        </RouterLink>
      </nav>

      <div class="nx-admin__sidebar-foot">
        <span>AI CLUB / NEXUS</span>
        <small>v1.0 · CONTENT ENGINE</small>
      </div>
    </aside>

    <div class="nx-admin__scrim" :class="{ show: navOpen }" @click="navOpen = false"></div>

    <div class="nx-admin__main">
      <header class="nx-admin__top">
        <button class="nx-admin__menu" aria-label="打开后台导航" @click="navOpen = !navOpen">
          ☰
        </button>
        <div>
          <p class="nx-kicker">{{ tabTitles[tab].eyebrow }}</p>
          <h1>{{ tabTitles[tab].title }}</h1>
        </div>
        <div class="nx-admin__top-actions">
          <RouterLink class="nx-admin__ghost" to="/">浏览官网 ↗</RouterLink>
          <el-button @click="logout">退出登录</el-button>
        </div>
      </header>

      <div class="nx-admin__content">
        <AdminSections v-if="tab === 'content'" @total="contentTotal = $event" />
        <AdminAccounts v-else-if="tab === 'accounts' && auth.user?.role === 'SUPER_ADMIN'" />
        <AdminAudit v-else-if="tab === 'audit'" />
      </div>
    </div>

    <el-dialog v-model="passwordDialog" title="修改密码" width="min(92vw,480px)">
      <form class="nx-stack-form" @submit.prevent="changePassword">
        <label>
          原密码
          <input
            v-model="passwordForm.oldPassword"
            type="password"
            required
            maxlength="72"
            autocomplete="current-password"
          />
        </label>
        <label>
          新密码（12–72 字符）
          <input
            v-model="passwordForm.newPassword"
            type="password"
            required
            minlength="12"
            maxlength="72"
            autocomplete="new-password"
          />
        </label>
        <p>修改后所有设备的旧登录会话将失效。</p>
        <button class="nx-btn nx-btn--primary" :disabled="busy">修改并重新登录</button>
      </form>
    </el-dialog>
  </div>
</template>
