<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvas = ref<HTMLCanvasElement>()
let frame = 0
let cleanup = () => {}

interface Node {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  tone: number
}

onMounted(() => {
  const el = canvas.value
  if (!el) return
  const context = el.getContext('2d')
  if (!context) return

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const fine = window.matchMedia('(pointer: fine)').matches
  let width = 0
  let height = 0
  let nodes: Node[] = []
  const pointer = { x: -9999, y: -9999, active: false }

  function resize() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    width = el!.clientWidth
    height = el!.clientHeight
    el!.width = Math.floor(width * dpr)
    el!.height = Math.floor(height * dpr)
    context!.setTransform(dpr, 0, 0, dpr, 0, 0)
    const count = Math.max(28, Math.min(88, Math.round((width * height) / 26000)))
    nodes = Array.from({ length: count }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.25,
      vy: (Math.random() - 0.5) * 0.25,
      r: Math.random() * 1.5 + 0.6,
      tone: Math.random(),
    }))
  }

  function draw() {
    const ctx = context!
    ctx.clearRect(0, 0, width, height)
    const link = Math.min(170, Math.max(110, width / 9))
    for (let i = 0; i < nodes.length; i++) {
      const a = nodes[i]
      for (let j = i + 1; j < nodes.length; j++) {
        const b = nodes[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const dist = Math.hypot(dx, dy)
        if (dist > link) continue
        const alpha = (1 - dist / link) * 0.22
        ctx.strokeStyle =
          a.tone > 0.55 ? `rgba(129, 230, 217, ${alpha})` : `rgba(167, 139, 250, ${alpha})`
        ctx.lineWidth = 0.6
        ctx.beginPath()
        ctx.moveTo(a.x, a.y)
        ctx.lineTo(b.x, b.y)
        ctx.stroke()
      }
    }
    for (const n of nodes) {
      const color =
        n.tone > 0.55 ? 'rgba(153, 246, 228, 0.85)' : 'rgba(196, 181, 253, 0.8)'
      ctx.fillStyle = color
      ctx.beginPath()
      ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2)
      ctx.fill()
      if (pointer.active) {
        const dx = n.x - pointer.x
        const dy = n.y - pointer.y
        const dist = Math.hypot(dx, dy)
        if (dist < 190) {
          const alpha = (1 - dist / 190) * 0.32
          ctx.strokeStyle = `rgba(94, 234, 212, ${alpha})`
          ctx.lineWidth = 0.7
          ctx.beginPath()
          ctx.moveTo(n.x, n.y)
          ctx.lineTo(pointer.x, pointer.y)
          ctx.stroke()
        }
      }
    }
  }

  function step() {
    for (const n of nodes) {
      n.x += n.vx
      n.y += n.vy
      if (n.x < -20) n.x = width + 20
      if (n.x > width + 20) n.x = -20
      if (n.y < -20) n.y = height + 20
      if (n.y > height + 20) n.y = -20
    }
    draw()
    frame = requestAnimationFrame(step)
  }

  function onPointer(event: PointerEvent) {
    const rect = el!.getBoundingClientRect()
    pointer.x = event.clientX - rect.left
    pointer.y = event.clientY - rect.top
    pointer.active = true
  }
  function onLeave() {
    pointer.active = false
    pointer.x = -9999
    pointer.y = -9999
  }

  resize()
  draw()
  if (!reduce) {
    frame = requestAnimationFrame(step)
    if (fine) {
      window.addEventListener('pointermove', onPointer, { passive: true })
      window.addEventListener('pointerleave', onLeave)
    }
  }

  const ro = new ResizeObserver(() => {
    resize()
    draw()
  })
  ro.observe(el)

  cleanup = () => {
    cancelAnimationFrame(frame)
    ro.disconnect()
    window.removeEventListener('pointermove', onPointer)
    window.removeEventListener('pointerleave', onLeave)
  }
})

onBeforeUnmount(() => cleanup())
</script>

<template>
  <canvas ref="canvas" class="nx-particles" aria-hidden="true"></canvas>
</template>
