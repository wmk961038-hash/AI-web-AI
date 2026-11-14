<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, mediaUrl, meta, message, type Content } from '../api'
import PageIntro from '../components/PageIntro.vue'

const props = defineProps<{ pageKey: string }>()
const item = ref<Content>()
const error = ref('')
const loading = ref(true)

const copy = computed(() =>
  props.pageKey === 'about'
    ? {
        kicker: 'ABOUT THE CLUB',
        title: '关于我们',
        desc: '一群对人工智能抱有好奇的人，在这里一起学习、创造与分享。',
        mark: 'ABOUT',
        glyph: '✳',
      }
    : {
        kicker: 'JOIN OUR COMMUNITY',
        title: '加入我们',
        desc: '带着你的好奇心而来，和一群认真又好玩的人并肩创造。',
        mark: 'JOIN',
        glyph: '✦',
      },
)

onMounted(async () => {
  try {
    item.value = (await api.get(`/public/pages/${props.pageKey}`)).data
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="nx-static">
    <PageIntro
      :kicker="copy.kicker"
      :title="item?.title || copy.title"
      :description="item?.summary || copy.desc"
      :mark="copy.mark"
      :glyph="copy.glyph"
    />

    <section class="nx-section nx-static__body">
      <p v-if="error" class="nx-notice nx-notice--error" role="alert">
        <span>{{ error }}</span>
      </p>

      <div v-if="loading" class="nx-article-skeleton">
        <span class="nx-skeleton__line nx-skeleton__line--lg"></span>
        <div class="nx-article-skeleton__block"></div>
      </div>

      <article v-if="item" class="nx-static__article">
        <div v-if="item.coverId" class="nx-static__cover">
          <img :src="mediaUrl(item.coverId)" :alt="item.title" />
          <span aria-hidden="true">{{ copy.mark }} / AI CLUB</span>
        </div>

        <div class="nx-static__content">
          <div class="nx-prose" v-html="item.body"></div>

          <div v-if="meta(item).joinUrl" class="nx-static__cta">
            <p class="nx-kicker">READY TO START?</p>
            <h3>下一段故事，等你一起写。</h3>
            <a
              class="nx-btn nx-btn--primary"
              :href="meta(item).joinUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              <span>前往报名</span>
              <i aria-hidden="true">↗</i>
            </a>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
