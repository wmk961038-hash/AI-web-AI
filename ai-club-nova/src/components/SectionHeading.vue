<script setup lang="ts">
defineProps<{
  index: string
  eyebrow: string
  title: string
  accent?: string
}>()
</script>

<template>
  <div class="nx-section-head">
    <span class="nx-section-head__index" aria-hidden="true">{{ index }}</span>
    <div class="nx-section-head__copy">
      <p class="nx-kicker">{{ eyebrow }}</p>
      <h2 class="nx-section-head__title">
        {{ title }}<span v-if="accent">{{ accent }}</span>
      </h2>
    </div>
    <div class="nx-section-head__action">
      <slot name="action"></slot>
    </div>
  </div>
</template>
