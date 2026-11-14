<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, meta, message, date, labels, activityState, type Content } from '../api'
const props = defineProps<{ preview?: boolean }>(),
  route = useRoute(),
  item = ref<Content>(),
  error = ref(''),
  zoom = ref('')
const m = computed(() => (item.value ? meta(item.value) : {}))
const zoomOpen = computed({
  get: () => !!zoom.value,
  set: (value: boolean) => {
    if (!value) zoom.value = ''
  },
})
onMounted(async () => {
  try {
    item.value = (
      await api.get(`/${props.preview ? 'admin' : 'public'}/contents/${route.params.id}`)
    ).data
    document.title = `${item.value!.title} · 人工智能社`
    document.querySelector('meta[name="description"]')?.setAttribute('content', item.value!.summary)
  } catch (e) {
    error.value = message(e)
  }
})
function onImage(e: MouseEvent) {
  if (e.target instanceof HTMLImageElement) zoom.value = e.target.src
}
</script>
<template>
  <section class="section article-wrap">
    <RouterLink class="back-link" :to="preview ? '/admin' : '/'"
      >← {{ preview ? '返回管理后台' : '返回官网' }}</RouterLink
    >
    <div v-if="preview" class="notice">管理员预览 · 当前内容尚未必公开，发布状态以后台为准。</div>
    <div v-if="error" class="notice error" role="alert">{{ error }}</div>
    <article v-if="item">
      <p class="eyebrow">{{ labels[item.type] }} / {{ item.category || 'AI CLUB' }}</p>
      <h1 class="article-title">{{ item.title }}</h1>
      <p class="article-meta">
        {{ date(item.publishedAt) }} <span v-if="m.sourceName"> · 来源：{{ m.sourceName }}</span
        ><span v-if="item.archived"> · 已归档</span>
      </p>
      <p class="article-summary">{{ item.summary }}</p>
      <div v-if="item.type === 'ACTIVITY'" class="facts">
        <p>活动状态：{{ activityState(item) }}</p>
        <p>
          时间：{{ m.startAt?.replace('T', ' ') || '待补充' }} —
          {{ m.endAt?.replace('T', ' ') || '待补充' }}
        </p>
        <p>地点：{{ m.location || '待补充' }}</p>
      </div>
      <div v-if="item.type === 'PERSON'" class="facts">
        {{ m.position || '身份待补充' }} · {{ m.cohort || '届别待补充' }}
      </div>
      <button
        v-if="item.coverId"
        class="image-button"
        @click="zoom = `/api/media/${item.coverId}`"
        aria-label="放大封面"
      >
        <img class="article-cover" :src="`/api/media/${item.coverId}`" :alt="item.title" />
      </button>
      <div class="prose" v-html="item.body" @click="onImage"></div>
      <p v-if="m.members" class="facts">参与成员：{{ m.members }}</p>
      <div class="external-links">
        <a v-if="m.repository" :href="m.repository" target="_blank" rel="noopener noreferrer"
          >代码仓库 ↗</a
        ><a v-if="m.demo" :href="m.demo" target="_blank" rel="noopener noreferrer">项目演示 ↗</a
        ><a v-if="m.sourceUrl" :href="m.sourceUrl" target="_blank" rel="noopener noreferrer"
          >阅读原文 ↗</a
        >
      </div>
      <div v-if="m.gallery?.length" class="gallery">
        <button
          v-for="(id, index) in m.gallery"
          :key="id"
          @click="zoom = `/api/media/${id}`"
          :aria-label="`放大照片 ${index + 1}`"
        >
          <img
            :src="`/api/media/${id}`"
            :alt="`${item.title} · 照片 ${index + 1}`"
            loading="lazy"
          />
        </button>
      </div>
      <p v-if="m.credit" class="article-meta">图片来源 / 署名：{{ m.credit }}</p>
    </article>
  </section>
  <el-dialog v-model="zoomOpen" width="min(94vw,1100px)" title="图片查看" @close="zoom = ''"
    ><img class="zoom-image" :src="zoom" alt="放大图片"
  /></el-dialog>
</template>
