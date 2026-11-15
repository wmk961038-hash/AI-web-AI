<script setup lang="ts">
import { mediaUrl, personRoleOf, type Content } from '../api'

defineProps<{ item: Content }>()
</script>

<template>
  <RouterLink v-tilt class="nx-person-card" :to="`/contents/${item.id}`">
    <div class="nx-person-card__media">
      <img
        v-if="item.coverId"
        :src="mediaUrl(item.coverId)"
        :alt="item.title"
        loading="lazy"
      />
      <div v-else class="nx-person-card__placeholder" aria-hidden="true">
        <span>✳</span>
        <small>PORTRAIT PENDING</small>
      </div>
      <span class="nx-person-card__badge">{{ item.category || '类别待补充' }}</span>
      <span class="nx-person-card__shine" aria-hidden="true"></span>
    </div>

    <div class="nx-person-card__body">
      <p class="nx-person-card__role">{{ personRoleOf(item) || '职务方向待补充' }}</p>
      <h3 class="nx-person-card__name">{{ item.title }}</h3>
      <span class="nx-person-card__cta">
        <span>查看简介</span>
        <i aria-hidden="true">↗</i>
      </span>
    </div>
  </RouterLink>
</template>
