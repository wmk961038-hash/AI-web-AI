<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, message, type Content } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
const home = ref<Content>(),
  news = ref<Content[]>([]),
  projects = ref<Content[]>([]),
  honors = ref<Content[]>([]),
  error = ref(''),
  loading = ref(true)
function retry() {
  window.location.reload()
}
const heroTitle = computed(() =>
  (home.value?.summary || '让好奇发生，让想法生长。').replace('，', '，\n'),
)
onMounted(async () => {
  try {
    const [h, n, p, a] = await Promise.all([
      api.get('/public/pages/home'),
      api.get('/public/contents', { params: { type: 'NEWS,ACTIVITY', size: 3 } }),
      api.get('/public/contents', { params: { type: 'PROJECT', featured: true, size: 3 } }),
      api.get('/public/contents', {
        params: { type: 'PROJECT', category: '竞赛获奖,科研成果', size: 3 },
      }),
    ])
    home.value = h.data
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
  <section class="hero">
    <div class="hero-grid">
      <div class="hero-copy">
        <p class="eyebrow light"><span class="live-dot"></span> EXPLORE · BUILD · CONNECT</p>
        <h1>{{ heroTitle }}</h1>
        <div v-if="home" class="hero-desc" v-html="home.body"></div>
        <p v-else class="hero-desc">聚焦人工智能、竞赛实践与科研探索。</p>
        <div class="hero-actions">
          <RouterLink class="button lime" to="/projects">探索我们的项目 ↗</RouterLink
          ><RouterLink class="text-link" to="/about">认识人工智能社 →</RouterLink>
        </div>
        <div class="hero-tags">
          <span>01 / 竞赛实践</span><span>02 / 科研探索</span><span>03 / 技术共创</span>
        </div>
      </div>
      <div class="hero-visual">
        <img v-if="home?.coverId" :src="`/api/media/${home.coverId}`" alt="社团精选图片" /><template
          v-else
          ><div class="orbit o1"></div>
          <div class="orbit o2"></div>
          <div class="orbit o3"></div>
          <div class="orbit-core">✳</div>
          <div class="orbit-label top">IDEAS INTO REALITY</div>
          <div class="orbit-label bottom">AI CLUB / 无限可能</div>
          <span class="coordinate">[ 让每一个想法，都有发生的可能 ]</span></template
        >
      </div>
    </div>
    <div class="hero-bottom">
      <span>{{ home?.title || '人工智能社' }} / AI CLUB</span><span>SCROLL TO EXPLORE ↓</span>
    </div>
  </section>
  <div class="ticker">
    <span>机器学习</span><i>✳</i><span>计算机视觉</span><i>✳</i><span>智能系统</span><i>✳</i
    ><span>科研与竞赛</span><i>✳</i><span>一起创造可能</span>
  </div>
  <div v-if="error" class="notice error" role="alert">
    {{ error }} <button @click="retry">重试</button>
  </div>
  <p v-if="loading" class="loading">正在连接社团动态…</p>
  <section class="section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">01 / WHAT'S HAPPENING</p>
        <h2>社团的每一次新鲜事<span>最新动态</span></h2>
      </div>
      <RouterLink class="text-link dark" to="/news">全部动态 ↗</RouterLink>
    </div>
    <div v-if="news.length" class="card-grid">
      <ContentCard v-for="item in news" :key="item.id" :item="item" />
    </div>
    <EmptyState v-else />
  </section>
  <section class="section projects-section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">02 / BUILT BY US</p>
        <h2>把灵感，做成真的<span>重点项目</span></h2>
      </div>
      <RouterLink class="text-link dark" to="/projects">探索全部项目 ↗</RouterLink>
    </div>
    <div v-if="projects.length" class="card-grid">
      <ContentCard v-for="item in projects" :key="item.id" :item="item" />
    </div>
    <EmptyState v-else text="重点项目待补充" />
  </section>
  <section class="section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">03 / MILESTONES</p>
        <h2>每一步探索，都有回响<span>荣誉成果</span></h2>
      </div>
    </div>
    <div v-if="honors.length" class="honor-list">
      <RouterLink v-for="(item, i) in honors" :key="item.id" :to="`/contents/${item.id}`"
        ><span>0{{ i + 1 }}</span>
        <h3>{{ item.title }}</h3>
        <small>{{ item.category }} ↗</small></RouterLink
      >
    </div>
    <EmptyState v-else text="真实成果与荣誉待补充" />
  </section>
  <section class="join-banner">
    <p class="eyebrow">YOUR NEXT CHAPTER STARTS HERE</p>
    <h2>下一个想法，<br />也许就来自你。</h2>
    <RouterLink class="button lime" to="/join">成为我们的一员 ↗</RouterLink
    ><span class="join-star">✳</span>
  </section>
</template>
