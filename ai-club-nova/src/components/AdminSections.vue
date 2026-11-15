<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, date, message, states, type Content, type Page } from '../api'
import { collectionSections, sections, type SectionDef } from '../sections'

const emit = defineEmits<{ total: [value: number] }>()

const pages = ref<Record<string, Content>>({})
const counts = ref<Record<string, number>>({})
const loading = ref(true)
const error = ref('')

const orderedSections = computed(() => sections)

function pageOf(section: SectionDef) {
  return section.pageKey ? pages.value[section.pageKey] : undefined
}

function countOf(section: SectionDef) {
  if (section.kind === 'page') {
    const page = pageOf(section)
    return page ? states[page.status] || page.status : '待补充'
  }
  const count = counts.value[section.key]
  return count === undefined ? '—' : `${count} 条内容`
}

function metaOf(section: SectionDef) {
  if (section.kind === 'page') {
    const page = pageOf(section)
    return page ? `更新于 ${date(page.updatedAt)}` : '页面尚未创建'
  }
  return section.types.includes('ACTIVITY') ? '新闻 · 活动' : '内容条目'
}

function previewTo(section: SectionDef) {
  if (section.kind === 'page') {
    const page = pageOf(section)
    return page ? `/admin/preview/${page.id}` : ''
  }
  return section.previewPath
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [pageRes, newsRes, projectRes, personRes] = await Promise.all([
      api.get<Page<Content>>('/admin/contents', { params: { type: 'PAGE', size: 100 } }),
      api.get<Page<Content>>('/admin/contents', { params: { type: 'NEWS,ACTIVITY', size: 1 } }),
      api.get<Page<Content>>('/admin/contents', { params: { type: 'PROJECT', size: 1 } }),
      api.get<Page<Content>>('/admin/contents', { params: { type: 'PERSON', size: 1 } }),
    ])
    const map: Record<string, Content> = {}
    for (const item of pageRes.data.items) map[item.category] = item
    pages.value = map
    counts.value = {
      news: newsRes.data.total,
      projects: projectRes.data.total,
      people: personRes.data.total,
    }
    emit(
      'total',
      newsRes.data.total +
        projectRes.data.total +
        personRes.data.total +
        Object.keys(map).filter((key) => key === 'about' || key === 'join').length,
    )
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="nx-sections">
    <div class="nx-sections__head">
      <div>
        <p class="nx-kicker">CONTENT SECTIONS / 五个栏目</p>
        <h2>选择要维护的栏目</h2>
      </div>
      <p class="nx-sections__intro">
        内容维护已按栏目归类。点击“编辑”进入对应编辑器，“预览”查看公开页面效果。
      </p>
    </div>

    <p v-if="error" class="nx-notice nx-notice--error" role="alert">
      <span>{{ error }}</span>
      <button @click="load">重试</button>
    </p>

    <div v-if="loading" class="nx-sections__grid">
      <div v-for="n in 5" :key="n" class="nx-section-card nx-section-card--skeleton"></div>
    </div>

    <div v-else class="nx-sections__grid">
      <article
        v-for="(section, index) in orderedSections"
        :key="section.key"
        class="nx-section-card"
        :class="`nx-section-card--${section.key}`"
      >
        <div class="nx-section-card__top">
          <span class="nx-section-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="nx-section-card__en">{{ section.english }}</span>
        </div>

        <div class="nx-section-card__body">
          <h3>{{ section.title }}</h3>
          <p>{{ section.desc }}</p>
        </div>

        <div class="nx-section-card__meta">
          <span class="nx-section-card__count">
            <i></i>{{ countOf(section) }}
          </span>
          <span class="nx-section-card__meta-text">{{ metaOf(section) }}</span>
        </div>

        <div class="nx-section-card__actions">
          <RouterLink class="nx-section-card__primary" :to="section.editPath">
            编辑
            <i aria-hidden="true">→</i>
          </RouterLink>
          <a
            v-if="previewTo(section)"
            class="nx-section-card__ghost"
            :href="previewTo(section)"
            target="_blank"
            rel="noopener noreferrer"
          >
            预览 ↗
          </a>
          <span v-else class="nx-section-card__ghost is-disabled">预览暂不可用</span>
        </div>
      </article>
    </div>

    <p class="nx-sections__foot">
      共 {{ collectionSections.length }} 个内容栏目 + 2 个固定页面；新增内容请进入对应栏目后使用右上角“新增内容”。
    </p>
  </section>
</template>
