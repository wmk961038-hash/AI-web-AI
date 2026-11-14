<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{ images: string[]; modelValue: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: number] }>()

const open = computed(() => props.modelValue >= 0 && props.images.length > 0)
const current = computed(() => props.images[props.modelValue] || '')
const total = computed(() => props.images.length)
const zoom = ref(1)

function close() {
  emit('update:modelValue', -1)
}

function go(delta: number) {
  if (props.images.length < 2) return
  const next = (props.modelValue + delta + props.images.length) % props.images.length
  zoom.value = 1
  emit('update:modelValue', next)
}

function pick(index: number) {
  zoom.value = 1
  emit('update:modelValue', index)
}

function onKey(event: KeyboardEvent) {
  if (!open.value) return
  if (event.key === 'Escape') close()
  if (event.key === 'ArrowLeft') go(-1)
  if (event.key === 'ArrowRight') go(1)
}

watch(open, (value) => {
  document.documentElement.style.overflow = value ? 'hidden' : ''
  zoom.value = 1
})

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  document.documentElement.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="lightbox">
      <div v-if="open" class="nx-lightbox" role="dialog" aria-modal="true" aria-label="图片查看">
        <button class="nx-lightbox__backdrop" aria-label="关闭图片查看" @click="close"></button>

        <div class="nx-lightbox__top">
          <span class="nx-kicker">IMAGE VIEWER</span>
          <span class="nx-lightbox__count">
            {{ String(modelValue + 1).padStart(2, '0') }}
            <i>/</i>
            {{ String(total).padStart(2, '0') }}
          </span>
          <button class="nx-lightbox__close" aria-label="关闭" @click="close">✕</button>
        </div>

        <div class="nx-lightbox__stage">
          <button
            v-if="total > 1"
            class="nx-lightbox__nav nx-lightbox__nav--prev"
            aria-label="上一张"
            @click="go(-1)"
          >
            ←
          </button>
          <div class="nx-lightbox__frame" :class="{ 'is-zoomed': zoom > 1 }">
            <img
              :src="current"
              alt="放大图片"
              :style="{ transform: `scale(${zoom})` }"
              @dblclick="zoom = zoom > 1 ? 1 : 1.8"
            />
          </div>
          <button
            v-if="total > 1"
            class="nx-lightbox__nav nx-lightbox__nav--next"
            aria-label="下一张"
            @click="go(1)"
          >
            →
          </button>
        </div>

        <div v-if="total > 1" class="nx-lightbox__thumbs">
          <button
            v-for="(src, index) in images"
            :key="`${src}-${index}`"
            :class="{ active: index === modelValue }"
            :aria-label="`查看第 ${index + 1} 张图片`"
            @click="pick(index)"
          >
            <img :src="src" alt="" loading="lazy" />
          </button>
        </div>
        <p class="nx-lightbox__hint">双击放大 · 方向键切换 · ESC 关闭</p>
      </div>
    </Transition>
  </Teleport>
</template>
