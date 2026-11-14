<script setup lang="ts">
defineProps<{ text?: string }>()
</script>
<template>
  <div class="empty-state">
    <span>✳</span>
    <h3>{{ text || '内容正在整理中' }}</h3>
    <p>相关资料待补充，欢迎稍后再来看看。</p>
  </div>
</template>
