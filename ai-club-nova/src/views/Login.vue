<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { message } from '../api'
import BrandMark from '../components/BrandMark.vue'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const error = ref('')
const busy = ref(false)

async function submit() {
  busy.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = String(route.query.redirect || '/admin')
    await router.push(redirect.startsWith('/admin') ? redirect : '/admin')
  } catch (e) {
    error.value = message(e)
  } finally {
    password.value = ''
    busy.value = false
  }
}
</script>

<template>
  <div class="nx-login">
    <section class="nx-login__story">
      <div class="nx-login__story-grid" aria-hidden="true"></div>
      <RouterLink class="nx-login__brand" to="/">
        <BrandMark />
        <span>人工智能社<small>CONTENT STUDIO</small></span>
      </RouterLink>

      <div class="nx-login__story-copy">
        <p class="nx-kicker nx-kicker--light">AI CLUB / CONTENT STUDIO</p>
        <h1>让好内容，<br /><span>持续发生。</span></h1>
        <p>记录社团的成长，连接每一位探索者。</p>
      </div>

      <div class="nx-login__story-foot">
        <span>✳</span>
        <span>CONTENT · PEOPLE · MEMORY</span>
      </div>

      <div class="nx-login__orb" aria-hidden="true">
        <span class="nx-login__orb-ring"></span>
        <span class="nx-login__orb-ring"></span>
        <span class="nx-login__orb-core">✳</span>
      </div>
    </section>

    <section class="nx-login__panel">
      <div class="nx-login__form-wrap">
        <p class="nx-kicker">WELCOME BACK</p>
        <h2>管理员登录</h2>
        <p class="nx-login__lead">使用个人管理员账号维护社团内容。</p>

        <form class="nx-login__form" @submit.prevent="submit">
          <label class="nx-login__field">
            <span>账号</span>
            <input
              id="username"
              v-model="username"
              required
              maxlength="64"
              autocomplete="username"
              placeholder="请输入管理员账号"
            />
            <i aria-hidden="true">@</i>
          </label>

          <label class="nx-login__field">
            <span>密码</span>
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              required
              maxlength="72"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
            <button
              type="button"
              class="nx-login__eye"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '◉' : '◎' }}
            </button>
          </label>

          <p v-if="error" class="nx-notice nx-notice--error" role="alert">
            <span>{{ error }}</span>
          </p>

          <button class="nx-btn nx-btn--primary nx-login__submit" :disabled="busy">
            <span>{{ busy ? '正在登录…' : '登录管理后台' }}</span>
            <i aria-hidden="true">→</i>
          </button>
        </form>

        <div class="nx-login__bottom">
          <RouterLink class="nx-link" to="/">浏览官网 <i>↗</i></RouterLink>
          <small>账号由超级管理员分配；忘记密码请联系社团管理员。</small>
        </div>
      </div>
    </section>
  </div>
</template>
