<script setup lang="ts">
defineProps<{
  kicker: string
  title: string
  description?: string
  mark?: string
  glyph?: string
}>()
</script>

<template>
  <header class="nx-page-intro">
    <div class="nx-page-intro__grid" aria-hidden="true"></div>
    <div class="nx-page-intro__inner">
      <p class="nx-kicker">{{ kicker }}</p>
      <h1 class="nx-page-intro__title">
        <span>{{ title }}</span>
        <i aria-hidden="true">{{ glyph || '✳' }}</i>
      </h1>
      <p v-if="description" class="nx-page-intro__desc">{{ description }}</p>
      <div class="nx-page-intro__line" aria-hidden="true"></div>
    </div>
    <span v-if="mark" class="nx-page-intro__mark" aria-hidden="true">{{ mark }}</span>
  </header>
</template>
