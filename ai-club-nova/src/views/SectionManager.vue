<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  api,
  date,
  labels,
  message,
  personRoleOf,
  states,
  type Content,
  type Page,
} from '../api'
import { sectionByKey } from '../sections'
import PaginationBar from '../components/PaginationBar.vue'

const route = useRoute()
const router = useRouter()
const section = computed(() => sectionByKey(String(route.params.section)))
const isPeople = computed(() => section.value?.key === 'people')
const addTo = computed(() =>
  isPeople.value ? '/admin/people/new' : section.value?.addPath || '/admin',
)
const items = ref<Content[]>([])
const total = ref(0)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const categoryOptions = ref<string[]>([])

const search = ref('')
const statusFilter = ref('')
const categoryFilter = ref('')
const dateFrom = ref('')
const dateTo = ref('')
const page = ref(1)
const pageSize = ref(20)
const mobileQuery = window.matchMedia('(max-width: 760px)')
const isMobile = ref(mobileQuery.matches)

const rangeStart = computed(() =>
  total.value ? (page.value - 1) * pageSize.value + 1 : 0,
)
const rangeEnd = computed(() => Math.min(page.value * pageSize.value, total.value))

let requestId = 0
let searchTimer: number | undefined

function syncMobile(event: MediaQueryListEvent | MediaQueryList) {
  isMobile.value = event.matches
}

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'DRAFT', label: '草稿' },
]

function editTo(item: Content) {
  return isPeople.value ? `/admin/people/${item.id}` : `/admin/edit/${item.id}`
}

function applyFilters() {
  if (dateFrom.value && dateTo.value && dateFrom.value > dateTo.value) {
    error.value = '开始日期不能晚于结束日期'
    return
  }
  page.value = 1
  void load()
}

function onSearchInput() {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(applyFilters, 300)
}

function resetFilters() {
  search.value = ''
  statusFilter.value = ''
  categoryFilter.value = ''
  dateFrom.value = ''
  dateTo.value = ''
  page.value = 1
  void load()
}

async function loadCategoryOptions() {
  const fallback = section.value?.categories || []
  if (!isPeople.value) {
    categoryOptions.value = fallback
    return
  }
  try {
    const { data } = await api.get<string[]>('/admin/people/categories')
    categoryOptions.value = [...new Set([...fallback, ...data])].sort((a, b) =>
      a.localeCompare(b, 'zh-CN'),
    )
  } catch {
    categoryOptions.value = fallback
  }
}

async function load() {
  if (!section.value || section.value.kind !== 'collection') {
    await router.replace('/admin')
    return
  }
  const currentRequest = ++requestId
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get<Page<Content>>('/admin/contents', {
      params: {
        type: section.value.types.join(','),
        page: page.value,
        size: pageSize.value,
        status: statusFilter.value || undefined,
        category: categoryFilter.value || undefined,
        keyword: search.value.trim() || undefined,
        dateFrom: dateFrom.value || undefined,
        dateTo: dateTo.value || undefined,
      },
    })
    if (currentRequest !== requestId) return
    const maxPage = Math.max(1, Math.ceil(data.total / pageSize.value))
    if (!data.items.length && page.value > maxPage) {
      page.value = maxPage
      void load()
      return
    }
    items.value = data.items
    total.value = data.total
  } catch (e) {
    if (currentRequest === requestId) error.value = message(e)
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

async function down(item: Content) {
  try {
    await ElMessageBox.confirm(
      '下架后该内容将从官网移除，并保留为草稿；之后可在编辑页重新发布。确定下架「' +
        item.title +
        '」？',
      '下架内容',
      { confirmButtonText: '确定下架', cancelButtonText: '取消', type: 'warning' },
    )
    busy.value = true
    await api.post(`/admin/contents/${item.id}/unpublish`, { version: item.version })
    ElMessage.success('已下架，内容已保留为草稿')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

async function publish(item: Content) {
  try {
    await ElMessageBox.confirm(
      '发布后「' + item.title + '」将立即出现在官网。确定发布？',
      '发布内容',
      { confirmButtonText: '确定发布', cancelButtonText: '取消' },
    )
    busy.value = true
    await api.post(`/admin/contents/${item.id}/publish`, { version: item.version })
    ElMessage.success('内容已发布')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

watch(
  () => route.params.section,
  () => {
    search.value = ''
    statusFilter.value = ''
    categoryFilter.value = ''
    dateFrom.value = ''
    dateTo.value = ''
    page.value = 1
    void loadCategoryOptions()
    void load()
  },
)

onMounted(() => {
  syncMobile(mobileQuery)
  mobileQuery.addEventListener('change', syncMobile)
  void loadCategoryOptions()
  void load()
})

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
  mobileQuery.removeEventListener('change', syncMobile)
})
</script>

<template>
  <div v-if="section" class="nx-manager">
    <header class="nx-editor__top">
      <div class="nx-editor__top-left">
        <RouterLink class="nx-back" to="/admin"><i>←</i> 返回内容管理</RouterLink>
        <div class="nx-editor__heading">
          <p class="nx-kicker">SECTION MANAGER / {{ section.english }}</p>
          <h1>{{ section.title }}</h1>
        </div>
      </div>
      <div class="nx-editor__top-actions">
        <span class="nx-manager__count">
          共 <b>{{ total }}</b> 条
        </span>
        <RouterLink class="nx-btn nx-btn--primary nx-manager__add" :to="addTo">
          <span>＋ 新增内容</span>
        </RouterLink>
      </div>
    </header>

    <div class="nx-manager__body">
      <section class="nx-manager__filters" aria-label="内容查询条件">
        <label class="nx-manager__field nx-manager__field--search">
          <span>关键词</span>
          <input
            v-model="search"
            :placeholder="isPeople ? '搜索姓名、类别或职务方向' : '搜索标题或摘要'"
            @input="onSearchInput"
            @keyup.enter="applyFilters"
          />
        </label>

        <label class="nx-manager__field">
          <span>状态</span>
          <select v-model="statusFilter" @change="applyFilters">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="nx-manager__field">
          <span>{{ isPeople ? '类别' : '分类' }}</span>
          <select v-model="categoryFilter" @change="applyFilters">
            <option value="">{{ isPeople ? '全部类别' : '全部分类' }}</option>
            <option v-for="option in categoryOptions" :key="option" :value="option">
              {{ option }}
            </option>
          </select>
        </label>

        <label class="nx-manager__field">
          <span>上传时间</span>
          <span class="nx-manager__dates">
            <input v-model="dateFrom" type="date" aria-label="开始日期" @change="applyFilters" />
            <i>—</i>
            <input v-model="dateTo" type="date" aria-label="结束日期" @change="applyFilters" />
          </span>
        </label>

        <button class="nx-manager__reset" type="button" @click="resetFilters">重置</button>
      </section>

      <div class="nx-manager__summary">
        <span>
          共 <b>{{ total }}</b> 条
          <template v-if="total"> · 当前 {{ rangeStart }}–{{ rangeEnd }}</template>
        </span>
        <span class="nx-manager__order-tip">置顶优先 · 上传时间倒序</span>
      </div>

      <p v-if="error" class="nx-notice nx-notice--error" role="alert">
        <span>{{ error }}</span>
        <button @click="load">重试</button>
      </p>

      <div v-if="isMobile" class="nx-manager__cards" v-loading="loading || busy">
        <article v-for="row in items" :key="row.id" class="nx-manager__card">
          <div class="nx-manager__card-top">
            <b>{{ row.title }}</b>
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="dark">
              {{ states[row.status] || row.status }}
            </el-tag>
          </div>
          <p class="nx-manager__card-meta">
            {{ labels[row.type] }} · {{ row.category || '未分类' }} · {{ date(row.createdAt) }}
            <template v-if="isPeople">
              · {{ personRoleOf(row) || '职务方向待补充' }}
            </template>
            <span v-if="row.featured"> · 首页推荐</span>
            <span v-if="row.pinned"> · 置顶</span>
          </p>
          <div class="nx-manager__card-actions">
            <el-button link type="primary" @click="router.push(editTo(row))">
              编辑
            </el-button>
            <RouterLink class="nx-manager__preview" :to="'/admin/preview/' + row.id">
              预览
            </RouterLink>
            <el-button
              v-if="row.status === 'PUBLISHED'"
              link
              type="warning"
              @click="down(row)"
            >
              下架（删除）
            </el-button>
            <el-button v-else link type="success" @click="publish(row)">发布</el-button>
          </div>
        </article>
        <p v-if="!items.length" class="nx-manager__empty">暂无符合条件的条目</p>
      </div>

      <div v-else class="nx-manager__table">
        <el-table
          :data="items"
          v-loading="loading || busy"
          empty-text="暂无符合条件的条目"
          row-key="id"
        >
          <el-table-column label="标题" min-width="320">
            <template #default="{ row }">
              <div class="nx-manager__title">
                <b>{{ row.title }}</b>
                <small>
                  {{ labels[row.type] }} · {{ row.category || '未分类' }}
                  <template v-if="isPeople">
                    · {{ personRoleOf(row) || '职务方向待补充' }}
                  </template>
                  <span v-if="row.featured">· 首页推荐</span>
                  <span v-if="row.pinned">· 置顶</span>
                </small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="140">
            <template #default="{ row }">{{ date(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="dark">
                {{ states[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="260">
            <template #default="{ row }">
              <div class="nx-manager__actions">
                <el-button link type="primary" @click="router.push(editTo(row))">
                  编辑
                </el-button>
                <RouterLink class="nx-manager__preview" :to="'/admin/preview/' + row.id">
                  预览
                </RouterLink>
                <el-button
                  v-if="row.status === 'PUBLISHED'"
                  link
                  type="warning"
                  @click="down(row)"
                >
                  下架（删除）
                </el-button>
                <el-button v-else link type="success" @click="publish(row)">发布</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <PaginationBar
        v-if="total > 0"
        v-model:page="page"
        v-model:page-size="pageSize"
        class="nx-pagination--admin"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="load"
      />

      <div class="nx-manager__foot">
        <RouterLink class="nx-link" to="/admin">返回内容管理 <i>←</i></RouterLink>
        <p>下架的内容会保留为草稿，可在编辑页重新发布。</p>
      </div>
    </div>
  </div>
</template>
