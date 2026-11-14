<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, labels, states, date, message, refreshCsrf, type Content } from '../api'
import { useAuth } from '../stores/auth'
import AdminAccounts from '../components/AdminAccounts.vue'
import AdminAudit from '../components/AdminAudit.vue'
import BrandMark from '../components/BrandMark.vue'

const auth = useAuth()
const router = useRouter()
const tab = ref('content')
const navOpen = ref(false)
const items = ref<Content[]>([])
const page = ref(1)
const total = ref(0)
const type = ref('')
const status = ref('')
const busy = ref(false)
const error = ref('')

const passwordDialog = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '' })

const tabTitles: Record<string, { title: string; eyebrow: string }> = {
  content: { title: '内容管理', eyebrow: 'CONTENT OVERVIEW' },
  audit: { title: '操作记录', eyebrow: 'ACTIVITY LOG' },
  accounts: { title: '账号管理', eyebrow: 'ACCOUNTS & ROLES' },
}

async function load() {
  busy.value = true
  error.value = ''
  try {
    const { data } = await api.get('/admin/contents', {
      params: {
        type: type.value || undefined,
        status: status.value || undefined,
        page: page.value,
        size: 20,
      },
    })
    items.value = data.items
    total.value = data.total
  } catch (e) {
    error.value = message(e)
  } finally {
    busy.value = false
  }
}

function filter() {
  page.value = 1
  void load()
}

function selectTab(name: string) {
  tab.value = name
  navOpen.value = false
  if (name === 'content') void load()
}

async function transition(c: Content, action: string) {
  try {
    await ElMessageBox.confirm(
      '确定' +
        { publish: '发布', unpublish: '下架', trash: '移入回收站', restore: '恢复为草稿' }[action] +
        '「' +
        c.title +
        '」？',
      '内容操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
    await api.post('/admin/contents/' + c.id + '/' + action, { version: c.version })
    ElMessage.success('操作成功')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  }
}

async function logout() {
  try {
    await auth.logout()
    await router.push('/login')
  } catch (e) {
    ElMessage.error(message(e))
  }
}

async function changePassword() {
  busy.value = true
  try {
    await api.post('/auth/password', passwordForm.value)
    auth.user = null
    await refreshCsrf()
    ElMessage.success('密码已修改，请重新登录')
    await router.push('/login')
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="nx-admin">
    <aside class="nx-admin__sidebar" :class="{ open: navOpen }">
      <RouterLink class="nx-admin__brand" to="/" aria-label="人工智能社首页">
        <BrandMark />
        <span>人工智能社<small>CONTENT STUDIO</small></span>
      </RouterLink>

      <div class="nx-admin__user">
        <span class="nx-admin__avatar">{{ auth.user?.username?.slice(0, 1).toUpperCase() }}</span>
        <div>
          <b>{{ auth.user?.username }}</b>
          <small>{{ auth.user?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}</small>
        </div>
        <i class="nx-admin__online" title="在线"></i>
      </div>

      <nav class="nx-admin__nav" aria-label="后台导航">
        <p class="nx-admin__nav-label">WORKSPACE</p>
        <button
          :class="{ active: tab === 'content' }"
          @click="selectTab('content')"
        >
          <span class="nx-admin__nav-icon">▤</span>
          <span>内容管理</span>
          <small>{{ total }}</small>
        </button>
        <button :class="{ active: tab === 'audit' }" @click="selectTab('audit')">
          <span class="nx-admin__nav-icon">◷</span>
          <span>操作记录</span>
        </button>
        <button
          v-if="auth.user?.role === 'SUPER_ADMIN'"
          :class="{ active: tab === 'accounts' }"
          @click="selectTab('accounts')"
        >
          <span class="nx-admin__nav-icon">♙</span>
          <span>账号管理</span>
        </button>

        <p class="nx-admin__nav-label nx-admin__nav-label--spaced">SYSTEM</p>
        <button @click="passwordDialog = true">
          <span class="nx-admin__nav-icon">⚙</span>
          <span>修改密码</span>
        </button>
        <RouterLink class="nx-admin__nav-link" to="/">
          <span class="nx-admin__nav-icon">◈</span>
          <span>浏览官网</span>
          <small>↗</small>
        </RouterLink>
      </nav>

      <div class="nx-admin__sidebar-foot">
        <span>AI CLUB / NEXUS</span>
        <small>v1.0 · CONTENT ENGINE</small>
      </div>
    </aside>

    <div class="nx-admin__scrim" :class="{ show: navOpen }" @click="navOpen = false"></div>

    <div class="nx-admin__main">
      <header class="nx-admin__top">
        <button class="nx-admin__menu" aria-label="打开后台导航" @click="navOpen = !navOpen">
          ☰
        </button>
        <div>
          <p class="nx-kicker">{{ tabTitles[tab].eyebrow }}</p>
          <h1>{{ tabTitles[tab].title }}</h1>
        </div>
        <div class="nx-admin__top-actions">
          <RouterLink class="nx-admin__ghost" to="/">浏览官网 ↗</RouterLink>
          <el-button @click="logout">退出登录</el-button>
        </div>
      </header>

      <div class="nx-admin__content">
        <AdminAccounts v-if="tab === 'accounts' && auth.user?.role === 'SUPER_ADMIN'" />
        <AdminAudit v-else-if="tab === 'audit'" />
        <template v-else>
          <div class="nx-admin__toolbar">
            <div class="nx-admin__filters">
              <el-select v-model="type" clearable placeholder="全部内容" @change="filter">
                <el-option
                  v-for="(label, key) in labels"
                  :key="key"
                  :label="label"
                  :value="key"
                />
              </el-select>
              <el-select
                v-model="status"
                clearable
                placeholder="全部状态（不含回收站）"
                @change="filter"
              >
                <el-option
                  v-for="(label, key) in states"
                  :key="key"
                  :label="label"
                  :value="key"
                />
              </el-select>
            </div>
            <div class="nx-admin__toolbar-actions">
              <span class="nx-admin__count"><b>{{ total }}</b> 条内容</span>
              <el-button type="primary" @click="router.push('/admin/edit/new')">
                ＋ 新建内容
              </el-button>
            </div>
          </div>

          <p class="nx-admin__hint">
            首页精选图和介绍在“页面设置”中维护；勾选项目“首页推荐”或动态“置顶”可调整首页展示。已发布内容保存修改后立即生效。
          </p>

          <p v-if="error" class="nx-notice nx-notice--error" role="alert">
            <span>{{ error }}</span>
            <button @click="load">重试</button>
          </p>

          <div class="nx-admin__table">
            <el-table :data="items" v-loading="busy" empty-text="暂无内容">
              <el-table-column label="标题" min-width="260">
                <template #default="{ row }">
                  <div class="nx-admin__title-cell">
                    <b>{{ row.title }}</b>
                    <small class="nx-admin__table-sub">
                      {{ labels[row.type] }} · {{ row.category || '未分类' }}
                      <span v-if="row.featured">· 首页推荐</span>
                      <span v-if="row.pinned">· 置顶</span>
                      <span v-if="row.archived">· 已归档</span>
                    </small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag
                    :type="
                      row.status === 'PUBLISHED'
                        ? 'success'
                        : row.status === 'TRASH'
                          ? 'danger'
                          : 'info'
                    "
                    effect="dark"
                  >
                    {{ states[row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="更新时间" width="130">
                <template #default="{ row }">{{ date(row.updatedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" min-width="300">
                <template #default="{ row }">
                  <div class="nx-admin__row-actions">
                    <el-button
                      v-if="row.status !== 'TRASH'"
                      link
                      type="primary"
                      @click="router.push('/admin/edit/' + row.id)"
                    >
                      编辑
                    </el-button>
                    <RouterLink class="nx-admin__row-link" :to="'/admin/preview/' + row.id">
                      预览
                    </RouterLink>
                    <el-button
                      v-if="row.status === 'DRAFT'"
                      link
                      type="success"
                      @click="transition(row, 'publish')"
                    >
                      发布
                    </el-button>
                    <el-button
                      v-if="row.status === 'PUBLISHED'"
                      link
                      type="warning"
                      @click="transition(row, 'unpublish')"
                    >
                      下架
                    </el-button>
                    <el-button
                      v-if="row.status !== 'TRASH' && row.type !== 'PAGE'"
                      link
                      type="danger"
                      @click="transition(row, 'trash')"
                    >
                      回收站
                    </el-button>
                    <el-button
                      v-if="row.status === 'TRASH'"
                      link
                      type="primary"
                      @click="transition(row, 'restore')"
                    >
                      恢复
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="total > 20" class="nx-pagination">
            <el-pagination
              v-model:current-page="page"
              :page-size="20"
              :total="total"
              layout="prev, pager, next, total"
              background
              @current-change="load"
            />
          </div>
        </template>
      </div>
    </div>

    <el-dialog v-model="passwordDialog" title="修改密码" width="min(92vw,480px)">
      <form class="nx-stack-form" @submit.prevent="changePassword">
        <label>
          原密码
          <input
            v-model="passwordForm.oldPassword"
            type="password"
            required
            maxlength="72"
            autocomplete="current-password"
          />
        </label>
        <label>
          新密码（12–72 字符）
          <input
            v-model="passwordForm.newPassword"
            type="password"
            required
            minlength="12"
            maxlength="72"
            autocomplete="new-password"
          />
        </label>
        <p>修改后所有设备的旧登录会话将失效。</p>
        <button class="nx-btn nx-btn--primary" :disabled="busy">修改并重新登录</button>
      </form>
    </el-dialog>
  </div>
</template>
