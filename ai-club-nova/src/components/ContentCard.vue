<script setup lang="ts">
import { computed } from 'vue'
import { labels, meta, date, activityState, mediaUrl, type Content } from '../api'

const props = defineProps<{ item: Content; index?: number }>()

const glyph = computed(() => {
  switch (props.item.type) {
    case 'PROJECT':
      return '⌬'
    case 'PERSON':
      return '☺'
    case 'ACTIVITY':
      return '◈'
    default:
      return '✦'
  }
})

const kicker = computed(() => {
  if (props.item.type === 'PERSON') return meta(props.item).cohort || '届别待补充'
  return date(props.item.publishedAt)
})

const badge = computed(() => props.item.category || labels[props.item.type] || 'AI CLUB')
</script>

<template>
  <RouterLink
    v-tilt
    class="nx-card"
    :class="`nx-card--${item.type.toLowerCase()}`"
    :to="`/contents/${item.id}`"
  >
    <div class="nx-card__media">
      <img
        v-if="item.coverId"
        :src="mediaUrl(item.coverId)"
        :alt="item.title"
        loading="lazy"
      />
      <div v-else class="nx-card__placeholder">
        <span class="nx-card__glyph" aria-hidden="true">{{ glyph }}</span>
        <small>{{ labels[item.type] }} / 图片待补充</small>
      </div>
      <span class="nx-card__badge">{{ badge }}</span>
      <span class="nx-card__shine" aria-hidden="true"></span>
      <span v-if="index !== undefined" class="nx-card__number" aria-hidden="true">
        {{ String(index + 1).padStart(2, '0') }}
      </span>
    </div>

    <div class="nx-card__body">
      <div class="nx-card__meta">
        <span>{{ kicker }}</span>
        <span v-if="item.type === 'ACTIVITY'" class="nx-card__state">· {{ activityState(item) }}</span>
        <span v-if="item.archived" class="nx-card__state">· 已归档</span>
      </div>
      <h3 class="nx-card__title">{{ item.title }}</h3>
      <p class="nx-card__summary">{{ item.summary || '介绍待补充' }}</p>
      <span class="nx-card__cta">
        <span>了解更多</span>
        <i aria-hidden="true">↗</i>
      </span>
    </div>
  </RouterLink>
</template>
