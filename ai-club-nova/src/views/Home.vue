<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, mediaUrl, message, type Content } from '../api'
import BrandMark from '../components/BrandMark.vue'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
import SectionHeading from '../components/SectionHeading.vue'
import SkeletonCard from '../components/SkeletonCard.vue'


const news = ref<Content[]>([])
const projects = ref<Content[]>([])
const honors = ref<Content[]>([])
const error = ref('')
const loading = ref(true)

function retry() {
  window.location.reload()
}

const heroTitle = computed(() =>
  ('让好奇发生，让想法生长。').replace('，', '，\n'),
)
const heroLines = computed(() => heroTitle.value.split('\n').filter(Boolean))

const modules = ['MACHINE LEARNING', 'COMPUTER VISION', 'INTELLIGENT SYSTEMS']

onMounted(async () => {
  try {
    const [n, p, a] = await Promise.all([
      api.get('/public/contents', {
        params: { type: 'NEWS,ACTIVITY', featured: true, size: 3 },
      }),
      api.get('/public/contents', {
        params: { type: 'PROJECT', category: '技术项目', featured: true, size: 3 },
      }),
      api.get('/public/contents', {
        params: {
          type: 'PROJECT',
          category: '竞赛获奖,科研成果',
          featured: true,
          size: 3,
        },
      }),
    ])
    news.value = n.data.items
    projects.value = p.data.items
    honors.value = a.data.items
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="nx-home">
    <section class="nx-hero">
      <div class="nx-hero__halo nx-hero__halo--a" aria-hidden="true"></div>
      <div class="nx-hero__halo nx-hero__halo--b" aria-hidden="true"></div>

      <div class="nx-hero__grid">
        <div class="nx-hero__copy">
          <p class="nx-kicker nx-kicker--light">
            <span class="nx-live"><i></i> LIVE</span>
            EXPLORE · BUILD · CONNECT
          </p>

          <h1 class="nx-hero__title">
            <span
              v-for="(line, index) in heroLines"
              :key="index"
              :class="{ 'is-gradient': index === heroLines.length - 1 && heroLines.length > 1 }"
            >
              {{ line }}
            </span>
          </h1>

          <div class="nx-hero__rich">
            <p>聚焦人工智能、竞赛实践与科研探索，把好奇心变成可以运行的作品。</p>
          </div>

          <div class="nx-hero__actions">
            <RouterLink class="nx-btn nx-btn--primary" to="/projects">
              <span>探索我们的项目</span>
              <i aria-hidden="true">↗</i>
            </RouterLink>
            <RouterLink class="nx-btn nx-btn--ghost" to="/about">
              <span>认识人工智能社</span>
              <i aria-hidden="true">→</i>
            </RouterLink>
          </div>

          <ul class="nx-hero__tags">
            <li><small>01</small><span>竞赛实践</span></li>
            <li><small>02</small><span>科研探索</span></li>
            <li><small>03</small><span>技术共创</span></li>
          </ul>
        </div>

        <div class="nx-hero__visual">
          <div class="nx-orbit">
            <div class="nx-orbit__ring nx-orbit__ring--1"></div>
            <div class="nx-orbit__ring nx-orbit__ring--2"></div>
            <div class="nx-orbit__ring nx-orbit__ring--3"></div>
            <div class="nx-orbit__scan" aria-hidden="true"></div>
            <div class="nx-orbit__core">
              <BrandMark />
              <span>AI CLUB</span>
            </div>
            <span class="nx-orbit__node nx-orbit__node--1">ML</span>
            <span class="nx-orbit__node nx-orbit__node--2">CV</span>
            <span class="nx-orbit__node nx-orbit__node--3">NLP</span>
            <span class="nx-orbit__node nx-orbit__node--4">∞</span>
            <p class="nx-orbit__label nx-orbit__label--top">IDEAS INTO REALITY</p>
            <p class="nx-orbit__label nx-orbit__label--bottom">无限可能 / 正在发生</p>
          </div>

          <div class="nx-hero__modules">
            <span v-for="(module, index) in modules" :key="module">
              <i>0{{ index + 1 }}</i>{{ module }}
            </span>
          </div>
        </div>
      </div>

      <div class="nx-hero__bottom">
        <span>{{ '人工智能社' }} / AI CLUB</span>
        <span class="nx-hero__scroll">SCROLL TO EXPLORE <i>↓</i></span>
        <span>ARTIFICIAL INTELLIGENCE CLUB</span>
      </div>
    </section>

    <div class="nx-ticker" aria-hidden="true">
      <div class="nx-ticker__track">
        <span v-for="loop in 2" :key="loop" class="nx-ticker__set">
          <span>机器学习</span><i>✳</i><span>计算机视觉</span><i>✳</i><span>智能系统</span
          ><i>✳</i><span>科研与竞赛</span><i>✳</i><span>一起创造可能</span><i>✳</i>
        </span>
      </div>
    </div>

    <div v-if="error" class="nx-notice nx-notice--error" role="alert">
      <span>{{ error }}</span>
      <button @click="retry">重新连接</button>
    </div>

    <section class="nx-section">
      <SectionHeading index="01" eyebrow="WHAT'S HAPPENING" title="社团的每一次新鲜事" accent="最新动态">
        <template #action>
          <RouterLink class="nx-link" to="/news">全部动态 <i>↗</i></RouterLink>
        </template>
      </SectionHeading>

      <div v-if="loading" class="nx-grid">
        <SkeletonCard v-for="n in 3" :key="n" />
      </div>
      <div v-else-if="news.length" class="nx-grid">
        <ContentCard v-for="(item, index) in news" :key="item.id" :item="item" :index="index" />
      </div>
      <EmptyState v-else />
    </section>

    <section class="nx-section nx-section--band">
      <SectionHeading index="02" eyebrow="BUILT BY US" title="把灵感，做成真的" accent="重点项目">
        <template #action>
          <RouterLink class="nx-link" to="/projects">探索全部项目 <i>↗</i></RouterLink>
        </template>
      </SectionHeading>

      <div v-if="loading" class="nx-grid">
        <SkeletonCard v-for="n in 3" :key="n" />
      </div>
      <div v-else-if="projects.length" class="nx-grid">
        <ContentCard
          v-for="(item, index) in projects"
          :key="item.id"
          :item="item"
          :index="index"
        />
      </div>
      <EmptyState v-else text="重点项目待补充" />
    </section>

    <section class="nx-section nx-honors">
      <SectionHeading index="03" eyebrow="MILESTONES" title="每一步探索，都有回响" accent="荣誉成果">
        <template #action>
          <RouterLink class="nx-link" to="/projects">更多成果 <i>↗</i></RouterLink>
        </template>
      </SectionHeading>

      <div v-if="loading" class="nx-honors__list">
        <div v-for="n in 3" :key="n" class="nx-honors__skeleton"></div>
      </div>
      <div v-else-if="honors.length" class="nx-honors__list">
        <RouterLink
          v-for="(item, index) in honors"
          :key="item.id"
          class="nx-honor"
          :to="`/contents/${item.id}`"
          v-tilt
        >
          <span class="nx-honor__index">{{ String(index + 1).padStart(2, '0') }}</span>
          <h3>{{ item.title }}</h3>
          <span class="nx-honor__meta">{{ item.category || '成果' }}</span>
          <span class="nx-honor__arrow">↗</span>
        </RouterLink>
      </div>
      <EmptyState v-else text="真实成果与荣誉待补充" />
    </section>

    <section class="nx-join">
      <div class="nx-join__inner">
        <div>
          <p class="nx-kicker">YOUR NEXT CHAPTER STARTS HERE</p>
          <h2>下一个想法，<br /><span>也许就来自你。</span></h2>
          <RouterLink class="nx-btn nx-btn--primary" to="/join">
            <span>成为我们的一员</span>
            <i aria-hidden="true">↗</i>
          </RouterLink>
        </div>
        <div class="nx-join__star" aria-hidden="true">
          <span>✳</span>
          <i>JOIN US</i>
        </div>
        <span class="nx-join__watermark" aria-hidden="true">JOIN</span>
      </div>
    </section>
  </div>
</template>
