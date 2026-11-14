<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, labels, states, date, message, refreshCsrf, type Content } from '../api'
import { useAuth } from '../stores/auth'
import AdminAccounts from '../components/AdminAccounts.vue'
import AdminAudit from '../components/AdminAudit.vue'
const auth = useAuth(),
  router = useRouter(),
  tab = ref('content'),
  items = ref<Content[]>([]),
  page = ref(1),
  total = ref(0),
  type = ref(''),
  status = ref(''),
  busy = ref(false),
  error = ref('')
const passwordDialog = ref(false),
  passwordForm = ref({ oldPassword: '', newPassword: '' })
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
  <div class="admin-shell">
    <aside class="admin-aside">
      <p class="eyebrow">CONTENT STUDIO</p>
      <h2>管理工作台</h2>
      <button :class="{ selected: tab === 'content' }" @click="selectTab('content')">
        ▤ 内容管理</button
      ><button :class="{ selected: tab === 'audit' }" @click="selectTab('audit')">◷ 操作记录</button
      ><button
        v-if="auth.user?.role === 'SUPER_ADMIN'"
        :class="{ selected: tab === 'accounts' }"
        @click="selectTab('accounts')"
      >
        ♙ 账号管理</button
      ><button @click="passwordDialog = true">⚙ 修改密码</button
      ><RouterLink class="aside-link" to="/">浏览官网 ↗</RouterLink>
    </aside>
    <div class="admin-main">
      <div class="admin-top">
        <div>
          <p class="eyebrow">
            {{ auth.user?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }} /
            {{ auth.user?.username }}
          </p>
          <h1>{{ tab === 'content' ? '内容管理' : tab === 'audit' ? '操作记录' : '账号管理' }}</h1>
        </div>
        <el-button @click="logout">退出登录</el-button>
      </div>
      <AdminAccounts v-if="tab === 'accounts' && auth.user?.role === 'SUPER_ADMIN'" /><AdminAudit
        v-else-if="tab === 'audit'"
      />
      <template v-else
        ><div class="admin-toolbar">
          <el-select v-model="type" clearable placeholder="全部内容" @change="filter"
            ><el-option
              v-for="(label, key) in labels"
              :key="key"
              :label="label"
              :value="key" /></el-select
          ><el-select
            v-model="status"
            clearable
            placeholder="全部状态（不含回收站）"
            @change="filter"
            ><el-option
              v-for="(label, key) in states"
              :key="key"
              :label="label"
              :value="key" /></el-select
          ><el-button type="primary" @click="router.push('/admin/edit/new')">＋ 新建内容</el-button>
        </div>
        <p class="admin-hint">
          首页精选图和介绍在“页面设置”中维护；勾选项目“首页推荐”或动态“置顶”可调整首页展示。已发布内容保存修改后立即生效。
        </p>
        <p v-if="error" class="notice error" role="alert">
          {{ error }} <button @click="load">重试</button>
        </p>
        <div class="table-scroll">
          <el-table :data="items" v-loading="busy" empty-text="暂无内容"
            ><el-table-column label="标题" min-width="210"
              ><template #default="{ row }"
                ><b>{{ row.title }}</b
                ><small class="table-sub"
                  >{{ labels[row.type] }} · {{ row.category || '未分类' }}
                  <span v-if="row.featured">· 首页推荐</span><span v-if="row.pinned">· 置顶</span
                  ><span v-if="row.archived">· 已归档</span></small
                ></template
              ></el-table-column
            ><el-table-column label="状态" width="95"
              ><template #default="{ row }"
                ><el-tag
                  :type="
                    row.status === 'PUBLISHED'
                      ? 'success'
                      : row.status === 'TRASH'
                        ? 'danger'
                        : 'info'
                  "
                  >{{ states[row.status] }}</el-tag
                ></template
              ></el-table-column
            ><el-table-column label="更新时间" width="120"
              ><template #default="{ row }">{{ date(row.updatedAt) }}</template></el-table-column
            ><el-table-column label="操作" min-width="270"
              ><template #default="{ row }"
                ><el-button
                  v-if="row.status !== 'TRASH'"
                  link
                  type="primary"
                  @click="router.push('/admin/edit/' + row.id)"
                  >编辑</el-button
                ><RouterLink class="admin-link" :to="'/admin/preview/' + row.id">预览</RouterLink
                ><el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="success"
                  @click="transition(row, 'publish')"
                  >发布</el-button
                ><el-button
                  v-if="row.status === 'PUBLISHED'"
                  link
                  type="warning"
                  @click="transition(row, 'unpublish')"
                  >下架</el-button
                ><el-button
                  v-if="row.status !== 'TRASH' && row.type !== 'PAGE'"
                  link
                  type="danger"
                  @click="transition(row, 'trash')"
                  >回收站</el-button
                ><el-button
                  v-if="row.status === 'TRASH'"
                  link
                  type="primary"
                  @click="transition(row, 'restore')"
                  >恢复</el-button
                ></template
              ></el-table-column
            ></el-table
          >
        </div>
        <el-pagination
          v-if="total > 20"
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </template>
    </div>
  </div>
  <el-dialog v-model="passwordDialog" title="修改密码" width="min(92vw,480px)"
    ><form class="stack-form" @submit.prevent="changePassword">
      <label
        >原密码<input
          v-model="passwordForm.oldPassword"
          type="password"
          required
          maxlength="72"
          autocomplete="current-password" /></label
      ><label
        >新密码（12–72 字符）<input
          v-model="passwordForm.newPassword"
          type="password"
          required
          minlength="12"
          maxlength="72"
          autocomplete="new-password"
      /></label>
      <p>修改后所有设备的旧登录会话将失效。</p>
      <button class="button primary" :disabled="busy">修改并重新登录</button>
    </form></el-dialog
  >
</template>
