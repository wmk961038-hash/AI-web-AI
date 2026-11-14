<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const el = ref<HTMLDivElement>()
let frame = 0
let target = { x: window.innerWidth / 2, y: window.innerHeight / 2 }
let current = { ...target }

function onMove(event: PointerEvent) {
  target = { x: event.clientX, y: event.clientY }
}

function tick() {
  current.x += (target.x - current.x) * 0.08
  current.y += (target.y - current.y) * 0.08
  if (el.value) {
    el.value.style.transform = `translate3d(${current.x - 260}px, ${current.y - 260}px, 0)`
  }
  frame = requestAnimationFrame(tick)
}

onMounted(() => {
  if (!window.matchMedia('(pointer: fine)').matches) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  window.addEventListener('pointermove', onMove, { passive: true })
  frame = requestAnimationFrame(tick)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  window.removeEventListener('pointermove', onMove)
})
</script>

<template>
  <div ref="el" class="nx-pointer-glow" aria-hidden="true"></div>
</template>
