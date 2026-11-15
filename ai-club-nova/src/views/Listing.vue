<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api, message, type Content, type Page } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
import PageIntro from '../components/PageIntro.vue'
import PaginationBar from '../components/PaginationBar.vue'
import PeopleCard from '../components/PeopleCard.vue'
import SkeletonCard from '../components/SkeletonCard.vue'

const props = defineProps<{ section: string }>()
const items = ref<Content[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const filter = ref('')
const category = ref('')
const categoryOptions = ref<string[]>([])
const error = ref('')
const loading = ref(false)

const rangeStart = computed(() =>
  total.value ? (page.value - 1) * pageSize.value + 1 : 0,
)
const rangeEnd = computed(() => Math.min(page.value * pageSize.value, total.value))

let requestId = 0

const config = computed(() => {
  if (props.section === 'news') {
    return {
      title: '新闻与活动',
      english: 'NEWS & EVENTS',
      desc: '记录每一次相遇，分享每一步成长。',
      mark: 'NEWS',
      glyph: '✳',
      types: [
        ['', '全部动态'],
        ['NEWS', '新闻动态'],
        ['ACTIVITY', '社团活动'],
      ] as [string, string][],
      categories: ['新闻动态', '活动预告', '活动回顾'],
    }
  }
  if (props.section === 'projects') {
    return {
      title: '项目与成果',
      english: 'PROJECTS & ACHIEVEMENTS',
      desc: '在实践中探索，在协作中创造。',
      mark: 'WORKS',
      glyph: '✳',
      types: [['PROJECT', '全部项目']] as [string, string][],
      categories: ['技术项目', '竞赛获奖', '科研成果'],
    }
  }
  return {
    title: '师生风采',
    english: 'PEOPLE & COMMUNITY',
    desc: '因为共同的热爱，在这里相遇。',
    mark: 'PEOPLE',
    glyph: '✳',
    types: [['PERSON', '全部成员']] as [string, string][],
    categories: [],
  }
})

async function load() {
  const currentRequest = ++requestId
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
    const { data } = await api.get<Page<Content>>('/public/contents', {
      params: {
        type,
        page: page.value,
        size: pageSize.value,
        category: category.value || undefined,
      },
    })
    if (currentRequest !== requestId) return
    const maxPage = Math.max(1, Math.ceil(data.total / pageSize.value))
    if (!data.items.length && page.value > maxPage) {
      page.value = maxPage
      void load()
      return
    }
    total.value = data.total
    items.value = data.items
  } catch (e) {
    if (currentRequest === requestId) error.value = message(e)
  } finally {
    if (currentRequest === requestId) loading.value = false
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

async function loadCategories() {
  if (props.section !== 'people') {
    categoryOptions.value = []
    return
  }
  try {
    categoryOptions.value = (await api.get('/public/people/categories')).data
  } catch {
    categoryOptions.value = []
  }
}

watch(
  () => props.section,
  () => {
    filter.value = ''
    category.value = ''
    page.value = 1
    pageSize.value = 12
    items.value = []
    total.value = 0
    void loadCategories()
    void load()
  },
)

onMounted(() => {
  void loadCategories()
  void load()
})
</script>

<template>
  <div class="nx-listing">
    <PageIntro
      :kicker="config.english"
      :title="config.title"
      :description="config.desc"
      :mark="config.mark"
      :glyph="config.glyph"
    />

    <section class="nx-section nx-listing__section">
      <div class="nx-toolbar" v-reveal>
        <div class="nx-segmented" role="group" :aria-label="`${config.title}分类筛选`">
          <button
            v-for="[value, label] in config.types"
            :key="value"
            :class="{ active: filter === value }"
            :aria-pressed="filter === value"
            @click="setFilter(value)"
          >
            {{ label }}
          </button>
        </div>

        <div class="nx-toolbar__right">
          <label
            v-if="section === 'projects' || section === 'news' || section === 'people'"
            class="nx-field nx-field--select"
          >
            <span>{{ section === 'people' ? '类别' : '分类' }}</span>
            <select
              v-model="category"
              :aria-label="section === 'people' ? '人物类别' : '内容分类'"
              @change="change"
            >
              <option value="">{{ section === 'people' ? '全部类别' : '全部分类' }}</option>
              <option
                v-for="option in section === 'people' ? categoryOptions : config.categories"
                :key="option"
              >
                {{ option }}
              </option>
            </select>
          </label>

          <span class="nx-toolbar__count">
            <template v-if="total">第 {{ rangeStart }}–{{ rangeEnd }} 条 / </template>
            <b>{{ total }}</b> 条内容
          </span>
        </div>
      </div>

      <p v-if="error" class="nx-notice nx-notice--error" role="alert">
        <span>{{ error }}</span>
        <button @click="load">重试</button>
      </p>

      <div v-if="loading" class="nx-grid nx-grid--listing">
        <SkeletonCard v-for="n in 6" :key="n" />
      </div>
      <div v-else-if="items.length" class="nx-grid nx-grid--listing">
        <template v-if="section === 'people'">
          <PeopleCard
            v-for="(item, index) in items"
            :key="item.id"
            :item="item"
            v-reveal="index * 60"
          />
        </template>
        <template v-else>
          <ContentCard
            v-for="(item, index) in items"
            :key="item.id"
            :item="item"
            :index="index"
            v-reveal="index * 60"
          />
        </template>
      </div>
      <EmptyState v-else-if="!error" />

      <PaginationBar
        v-if="total > 0"
        v-model:page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </section>
  </div>
</template>
