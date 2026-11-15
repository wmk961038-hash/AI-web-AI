<script setup lang="ts">
import { computed, ref } from 'vue'
import { mediaUrl, personRoleOf, type Content } from '../api'
import ImageLightbox from './ImageLightbox.vue'

const props = defineProps<{ item: Content }>()
const viewer = ref(-1)

const images = computed(() => (props.item.coverId ? [mediaUrl(props.item.coverId)] : []))
</script>

<template>
  <article class="nx-person-profile" v-reveal>
    <div class="nx-person-profile__media">
      <button
        v-if="item.coverId"
        type="button"
        aria-label="放大人物图片"
        @click="viewer = 0"
      >
        <img :src="mediaUrl(item.coverId)" :alt="item.title" />
        <span aria-hidden="true">⤢</span>
      </button>
      <div v-else class="nx-person-profile__placeholder" aria-hidden="true">
        <span>✳</span>
        <small>PORTRAIT PENDING</small>
      </div>
    </div>

    <div class="nx-person-profile__info">
      <p class="nx-kicker">PEOPLE &amp; COMMUNITY</p>
      <span class="nx-person-profile__category">{{ item.category || '类别待补充' }}</span>
      <h1>{{ item.title }}</h1>
      <p class="nx-person-profile__role">{{ personRoleOf(item) || '职务方向待补充' }}</p>

      <div class="nx-person-profile__bio">
        <p class="nx-person-profile__label">简介</p>
        <div v-if="item.body && item.body.replace(/<[^>]*>/g, '').trim()" class="nx-prose" v-html="item.body"></div>
        <p v-else class="nx-person-profile__empty">简介待补充。</p>
      </div>
    </div>

    <ImageLightbox v-model="viewer" :images="images" />
  </article>
</template>
