<script setup lang="ts">
import { labels, meta, date, activityState, type Content } from '../api'
defineProps<{ item: Content }>()
</script>
<template>
  <RouterLink class="content-card" :to="`/contents/${item.id}`"
    ><div class="card-image" :class="`visual-${item.type.toLowerCase()}`">
      <img
        v-if="item.coverId"
        :src="`/api/media/${item.coverId}`"
        :alt="item.title"
        loading="lazy"
      />
      <div v-else class="placeholder-art">
        <span>{{ item.type === 'PERSON' ? '☺' : '✳' }}</span
        ><small>图片待补充</small>
      </div>
      <span class="image-label">{{ item.category || labels[item.type] }}</span>
    </div>
    <div class="card-info">
      <div class="eyebrow">
        {{ item.type === 'PERSON' ? meta(item).cohort || '届别待补充' : date(item.publishedAt) }}
        <span v-if="item.type === 'ACTIVITY'">· {{ activityState(item) }}</span
        ><span v-if="item.archived"> · 已归档</span>
      </div>
      <h3>{{ item.title }}</h3>
      <p>{{ item.summary || '介绍待补充' }}</p>
      <span class="card-arrow">了解更多 ↗</span>
    </div></RouterLink
  >
</template>
