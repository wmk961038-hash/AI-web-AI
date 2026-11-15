<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, meta, message, date, labels, activityState, mediaUrl, type Content } from '../api'
import ImageLightbox from '../components/ImageLightbox.vue'
import PersonProfile from '../components/PersonProfile.vue'

const props = defineProps<{ preview?: boolean }>()
const route = useRoute()
const item = ref<Content>()
const error = ref('')
const loading = ref(true)
const progress = ref(0)

const viewer = ref<{ images: string[]; index: number }>({ images: [], index: -1 })
const gallery = computed(() => (item.value ? meta(item.value).gallery || [] : []))
const m = computed(() => (item.value ? meta(item.value) : {}))

const hasAside = computed(() => {
  if (!item.value) return false
  const md = m.value
  return (
    item.value.type === 'ACTIVITY' ||
    !!md.members ||
    !!md.repository ||
    !!md.demo ||
    !!md.sourceUrl ||
    !!md.credit
  )
})

function openCover() {
  if (!item.value?.coverId) return
  viewer.value = { images: [mediaUrl(item.value.coverId)], index: 0 }
}

function openGallery(index: number) {
  viewer.value = { images: gallery.value.map((id) => mediaUrl(id)), index }
}

function updateViewer(value: number) {
  viewer.value = { ...viewer.value, index: value }
}

function onImage(event: MouseEvent) {
  if (event.target instanceof HTMLImageElement) {
    viewer.value = { images: [event.target.src], index: 0 }
  }
}

function onScroll() {
  const el = document.documentElement
  const max = el.scrollHeight - window.innerHeight
  progress.value = max > 0 ? Math.min(1, el.scrollTop / max) : 0
}

onMounted(async () => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  try {
    item.value = (
      await api.get(`/${props.preview ? 'admin' : 'public'}/contents/${route.params.id}`)
    ).data
    document.title = `${item.value!.title} · 人工智能社`
    document.querySelector('meta[name="description"]')?.setAttribute('content', item.value!.summary)
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <div class="nx-detail">
    <div class="nx-reading" aria-hidden="true">
      <span :style="{ transform: `scaleX(${progress})` }"></span>
    </div>

    <div class="nx-detail__top">
      <RouterLink class="nx-back" :to="preview ? '/admin' : '/'">
        <i aria-hidden="true">←</i>
        {{ preview ? '返回管理后台' : '返回官网' }}
      </RouterLink>
      <span v-if="item" class="nx-detail__type">{{ labels[item.type] }}</span>
    </div>

    <div v-if="preview" class="nx-notice nx-notice--preview">
      <span>预览模式</span>
      管理员预览 · 当前内容尚未必公开，发布状态以后台为准。
    </div>

    <p v-if="error" class="nx-notice nx-notice--error" role="alert">
      <span>{{ error }}</span>
      <RouterLink to="/">返回首页</RouterLink>
    </p>

    <div v-if="loading" class="nx-article-skeleton" aria-hidden="true">
      <span class="nx-skeleton__line nx-skeleton__line--sm"></span>
      <span class="nx-skeleton__line nx-skeleton__line--xl"></span>
      <span class="nx-skeleton__line nx-skeleton__line--lg"></span>
      <div class="nx-article-skeleton__block"></div>
    </div>

    <PersonProfile v-if="item && item.type === 'PERSON'" :item="item" />

    <article v-else-if="item" class="nx-article">
      <header class="nx-article__head" v-reveal>
        <p class="nx-kicker">{{ labels[item.type] }} / {{ item.category || 'AI CLUB' }}</p>
        <h1 class="nx-article__title">{{ item.title }}</h1>
        <div class="nx-article__meta">
          <span>{{ date(item.publishedAt) }}</span>
          <span v-if="m.sourceName">来源：{{ m.sourceName }}</span>
          <span v-if="item.archived">已归档</span>
          <span v-if="item.category">{{ item.category }}</span>
        </div>
        <p class="nx-article__summary">{{ item.summary }}</p>
      </header>

      <button
        v-if="item.coverId"
        class="nx-article__cover"
        aria-label="放大封面"
        @click="openCover"
      >
        <img :src="mediaUrl(item.coverId)" :alt="item.title" />
        <span class="nx-article__cover-hint">点击查看大图 ⤢</span>
      </button>

      <div class="nx-article__body-grid" :class="{ 'nx-article__body-grid--full': !hasAside }">
        <aside v-if="hasAside" class="nx-article__aside">
          <div v-if="item.type === 'ACTIVITY'" class="nx-facts">
            <p class="nx-facts__title">活动信息</p>
            <dl>
              <div>
                <dt>状态</dt>
                <dd><span class="nx-status-dot"></span>{{ activityState(item) }}</dd>
              </div>
              <div>
                <dt>开始</dt>
                <dd>{{ m.startAt?.replace('T', ' ') || '待补充' }}</dd>
              </div>
              <div>
                <dt>结束</dt>
                <dd>{{ m.endAt?.replace('T', ' ') || '待补充' }}</dd>
              </div>
              <div>
                <dt>地点</dt>
                <dd>{{ m.location || '待补充' }}</dd>
              </div>
            </dl>
          </div>

          <div v-if="m.members" class="nx-side-note">
            <p>参与成员</p>
            <span>{{ m.members }}</span>
          </div>

          <div v-if="m.repository || m.demo || m.sourceUrl" class="nx-side-links">
            <p>相关链接</p>
            <a
              v-if="m.repository"
              :href="m.repository"
              target="_blank"
              rel="noopener noreferrer"
            >
              代码仓库 <i>↗</i>
            </a>
            <a v-if="m.demo" :href="m.demo" target="_blank" rel="noopener noreferrer">
              项目演示 <i>↗</i>
            </a>
            <a v-if="m.sourceUrl" :href="m.sourceUrl" target="_blank" rel="noopener noreferrer">
              阅读原文 <i>↗</i>
            </a>
          </div>

          <div v-if="m.credit" class="nx-side-note">
            <p>图片来源 / 署名</p>
            <span>{{ m.credit }}</span>
          </div>
        </aside>

        <div class="nx-article__main">
          <div class="nx-prose" v-html="item.body" @click="onImage"></div>

          <div v-if="m.gallery?.length" class="nx-gallery">
            <div class="nx-gallery__head">
              <h2>活动照片</h2>
              <span>{{ m.gallery.length }} 张</span>
            </div>
            <div class="nx-gallery__grid">
              <button
                v-for="(id, index) in m.gallery"
                :key="id"
                :aria-label="`放大照片 ${index + 1}`"
                @click="openGallery(index)"
              >
                <img :src="mediaUrl(id)" :alt="`${item.title} · 照片 ${index + 1}`" loading="lazy" />
                <span aria-hidden="true">⤢</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </article>

    <ImageLightbox
      :images="viewer.images"
      :model-value="viewer.index"
      @update:model-value="updateViewer"
    />
  </div>
</template>
