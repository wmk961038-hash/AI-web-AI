<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { message } from '../api'
const auth = useAuth(),
  route = useRoute(),
  router = useRouter(),
  username = ref(''),
  password = ref(''),
  error = ref(''),
  busy = ref(false)
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
  <section class="login-section">
    <div class="login-story">
      <p class="eyebrow light">AI CLUB / CONTENT STUDIO</p>
      <h1>让好内容，<br />持续发生。</h1>
      <p>记录社团的成长，连接每一位探索者。</p>
      <span>✳</span>
    </div>
    <form class="login-form" @submit.prevent="submit">
      <p class="eyebrow">WELCOME BACK</p>
      <h2>管理员登录</h2>
      <p>使用个人管理员账号维护社团内容。</p>
      <label for="username">账号</label
      ><input
        id="username"
        v-model="username"
        required
        maxlength="64"
        autocomplete="username"
        placeholder="请输入管理员账号"
      /><label for="password">密码</label
      ><input
        id="password"
        v-model="password"
        type="password"
        required
        maxlength="72"
        autocomplete="current-password"
        placeholder="请输入密码"
      />
      <p v-if="error" class="notice error" role="alert">{{ error }}</p>
      <button class="button primary" :disabled="busy">
        {{ busy ? '正在登录…' : '登录管理后台 →' }}</button
      ><RouterLink class="browse-link" to="/">浏览官网 ↗</RouterLink
      ><small>账号由超级管理员分配；忘记密码请联系社团管理员。</small>
    </form>
  </section>
</template>
