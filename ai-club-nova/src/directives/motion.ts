import type { Directive } from 'vue'

const reduceMotion =
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

let observer: IntersectionObserver | null = null

function getObserver() {
  if (observer || typeof window === 'undefined') return observer
  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        ;(entry.target as HTMLElement).classList.add('nx-reveal--in')
        observer!.unobserve(entry.target)
      }
    },
    { rootMargin: '0px 0px -8% 0px', threshold: 0.08 },
  )
  return observer
}

/** v-reveal / v-reveal="120" — scroll-triggered entrance, delay in ms. */
export const reveal: Directive<HTMLElement, number | undefined> = {
  mounted(el, binding) {
    el.classList.add('nx-reveal')
    el.style.setProperty('--reveal-delay', `${binding.value ?? 0}ms`)
    if (reduceMotion || !('IntersectionObserver' in window)) {
      el.classList.add('nx-reveal--in')
      return
    }
    getObserver()?.observe(el)
  },
  unmounted(el) {
    observer?.unobserve(el)
  },
}

interface TiltState {
  move: (event: PointerEvent) => void
  leave: () => void
}

const tiltStates = new WeakMap<HTMLElement, TiltState>()

/** v-tilt — subtle pointer-tracked 3D tilt for cards. */
export const tilt: Directive<HTMLElement, void> = {
  mounted(el) {
    if (reduceMotion || !window.matchMedia('(pointer: fine)').matches) return
    el.classList.add('nx-tilt')
    const move = (event: PointerEvent) => {
      const rect = el.getBoundingClientRect()
      const px = (event.clientX - rect.left) / rect.width
      const py = (event.clientY - rect.top) / rect.height
      el.style.setProperty('--tilt-x', `${(0.5 - py) * 7}deg`)
      el.style.setProperty('--tilt-y', `${(px - 0.5) * 9}deg`)
      el.style.setProperty('--glow-x', `${px * 100}%`)
      el.style.setProperty('--glow-y', `${py * 100}%`)
    }
    const leave = () => {
      el.style.setProperty('--tilt-x', '0deg')
      el.style.setProperty('--tilt-y', '0deg')
    }
    el.addEventListener('pointermove', move)
    el.addEventListener('pointerleave', leave)
    tiltStates.set(el, { move, leave })
  },
  unmounted(el) {
    const state = tiltStates.get(el)
    if (!state) return
    el.removeEventListener('pointermove', state.move)
    el.removeEventListener('pointerleave', state.leave)
    tiltStates.delete(el)
  },
}
