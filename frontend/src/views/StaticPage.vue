<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, meta, message, type Content } from '../api'
const props = defineProps<{ pageKey: string }>(),
  item = ref<Content>(),
  error = ref('')
onMounted(async () => {
  try {
    item.value = (await api.get(`/public/pages/${props.pageKey}`)).data
  } catch (e) {
    error.value = message(e)
  }
})
</script>
<template>
  <div class="page-intro">
    <p class="eyebrow">{{ pageKey === 'about' ? 'ABOUT THE CLUB' : 'JOIN OUR COMMUNITY' }}</p>
    <h1>{{ item?.title || (pageKey === 'about' ? '关于我们' : '加入我们') }}<span>✳</span></h1>
    <p>{{ item?.summary || '相关资料待补充' }}</p>
  </div>
  <section class="section article-wrap">
    <p v-if="error" class="notice error">{{ error }}</p>
    <template v-if="item"
      ><img
        v-if="item.coverId"
        class="article-cover"
        :src="`/api/media/${item.coverId}`"
        :alt="item.title"
      />
      <div class="prose" v-html="item.body"></div>
      <a
        v-if="meta(item).joinUrl"
        class="button primary"
        :href="meta(item).joinUrl"
        target="_blank"
        rel="noopener noreferrer"
        >前往报名 ↗</a
      ></template
    >
  </section>
</template>
