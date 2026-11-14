<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, message, type Content } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
const props = defineProps<{ section: string }>(),
  items = ref<Content[]>([]),
  total = ref(0),
  page = ref(1),
  filter = ref(''),
  category = ref(''),
  cohort = ref(''),
  error = ref(''),
  loading = ref(false)
const config = computed(() =>
  props.section === 'news'
    ? {
        title: '新闻与活动',
        english: 'NEWS & EVENTS',
        desc: '记录每一次相遇，分享每一步成长。',
        types: [
          ['', '全部动态'],
          ['NEWS', '新闻动态'],
          ['ACTIVITY', '社团活动'],
        ],
      }
    : props.section === 'projects'
      ? {
          title: '项目与成果',
          english: 'PROJECTS & ACHIEVEMENTS',
          desc: '在实践中探索，在协作中创造。',
          types: [['PROJECT', '全部项目']],
        }
      : {
          title: '师生风采',
          english: 'PEOPLE & COMMUNITY',
          desc: '因为共同的热爱，在这里相遇。',
          types: [['PERSON', '全部成员']],
        },
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const type =
      filter.value ||
      (props.section === 'news'
        ? 'NEWS,ACTIVITY'
        : props.section === 'projects'
          ? 'PROJECT'
          : 'PERSON')
    const { data } = await api.get('/public/contents', {
      params: {
        type,
        page: page.value,
        size: 12,
        category: category.value || undefined,
        cohort: cohort.value || undefined,
      },
    })
    total.value = data.total
    items.value = data.items
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}
function setFilter(value: string) {
  filter.value = value
  change()
}
function change() {
  page.value = 1
  void load()
}
onMounted(load)
</script>
<template>
  <div class="page-intro">
    <p class="eyebrow">{{ config.english }}</p>
    <h1>{{ config.title }}<span>↗</span></h1>
    <p>{{ config.desc }}</p>
  </div>
  <section class="section listing-section">
    <div class="filters">
      <button
        v-for="[value, label] in config.types"
        :key="value"
        :class="{ selected: filter === value }"
        @click="setFilter(value)"
      >
        {{ label }}</button
      ><select
        v-if="section === 'projects'"
        v-model="category"
        @change="change"
        aria-label="成果分类"
      >
        <option value="">全部分类</option>
        <option>技术项目</option>
        <option>竞赛获奖</option>
        <option>科研成果</option></select
      ><select v-if="section === 'news'" v-model="category" @change="change" aria-label="动态分类">
        <option value="">全部分类</option>
        <option>新闻动态</option>
        <option>活动预告</option>
        <option>活动回顾</option></select
      ><input
        v-if="section === 'people'"
        v-model="cohort"
        @change="change"
        placeholder="按届别筛选，如 2026"
        aria-label="届别筛选"
      /><span class="result-count">{{ total }} 条内容</span>
    </div>
    <p v-if="error" class="notice error" role="alert">
      {{ error }} <button @click="load">重试</button>
    </p>
    <p v-if="loading" class="loading">正在加载…</p>
    <div v-else-if="items.length" class="card-grid">
      <ContentCard v-for="item in items" :key="item.id" :item="item" />
    </div>
    <EmptyState v-else-if="!error" /><el-pagination
      v-if="total > 12"
      v-model:current-page="page"
      :page-size="12"
      :total="total"
      layout="prev, pager, next"
      @current-change="load"
    />
  </section>
</template>
