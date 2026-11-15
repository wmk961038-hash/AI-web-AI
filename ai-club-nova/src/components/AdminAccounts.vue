<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, message, refreshCsrf, type Account, type Page } from '../api'
import { useAuth } from '../stores/auth'
import PaginationBar from './PaginationBar.vue'

const auth = useAuth()
const router = useRouter()
const items = ref<Account[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const busy = ref(false)
const error = ref('')
const dialog = ref(false)
const form = ref({ username: '', password: '', role: 'ADMIN' })

let requestId = 0

async function load() {
  const currentRequest = ++requestId
  busy.value = true
  error.value = ''
  try {
    const { data } = await api.get<Page<Account>>('/admin/accounts', {
      params: { page: page.value, size: pageSize.value },
    })
    if (currentRequest !== requestId) return
    const maxPage = Math.max(1, Math.ceil(data.total / pageSize.value))
    if (!data.items.length && page.value > maxPage) {
      page.value = maxPage
      await load()
      return
    }
    items.value = data.items
    total.value = data.total
  } catch (e) {
    if (currentRequest === requestId) error.value = message(e)
  } finally {
    if (currentRequest === requestId) busy.value = false
  }
}

async function create() {
  busy.value = true
  try {
    await api.post('/admin/accounts', form.value)
    form.value = { username: '', password: '', role: 'ADMIN' }
    dialog.value = false
    ElMessage.success('账号已创建')
    await load()
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

async function change(a: Account, enabled: boolean, role: string) {
  try {
    await ElMessageBox.confirm(
      '确认修改 ' + a.username + ' 的启用状态或角色？旧登录会话会失效。',
      '账号管理',
      { confirmButtonText: '确认', cancelButtonText: '取消' },
    )
    await api.put('/admin/accounts/' + a.id, { enabled, role })
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  }
}

async function reset(a: Account) {
  try {
    const { value } = await ElMessageBox.prompt(
      '为 ' + a.username + ' 设置新密码（12–72 字符）。该账号原有会话将失效。',
      '重置密码',
      {
        inputType: 'password',
        inputPattern: /^.{12,72}$/,
        inputErrorMessage: '密码需为 12–72 字符',
        confirmButtonText: '重置',
        cancelButtonText: '取消',
      },
    )
    await api.post('/admin/accounts/' + a.id + '/password', { password: value })
    ElMessage.success('密码已重置')
    if (a.id === auth.user?.id) {
      auth.user = null
      await refreshCsrf()
      await router.push('/login')
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(message(e))
  }
}

onMounted(load)
</script>

<template>
  <section class="nx-admin__panel">
    <div class="nx-admin__panel-head">
      <div>
        <p class="nx-kicker">ACCOUNTS & ROLES</p>
        <h2>账号、角色与密码</h2>
      </div>
      <el-button type="primary" @click="dialog = true">创建账号</el-button>
    </div>
    <p class="nx-admin__hint">
      创建个人账号、启用或停用账号、调整角色及重置密码。不能停用或降级自己。重置自己的密码后需要重新登录。
    </p>
    <p v-if="error" class="nx-notice nx-notice--error">
      <span>{{ error }}</span>
      <button @click="load">重试</button>
    </p>

    <div class="nx-admin__table">
      <el-table :data="items" v-loading="busy">
        <el-table-column prop="username" label="账号" min-width="170" />
        <el-table-column label="角色" min-width="130">
          <template #default="{ row }">
            <span class="nx-role" :class="{ 'nx-role--super': row.role === 'SUPER_ADMIN' }">
              {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="nx-user-state" :class="{ 'is-off': !row.enabled }">
              <i></i>{{ row.enabled ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="{ row }">
            <div class="nx-admin__row-actions">
              <el-button link @click="reset(row)">重置密码</el-button>
              <el-button
                v-if="row.id !== auth.user?.id"
                link
                @click="change(row, !row.enabled, row.role)"
              >
                {{ row.enabled ? '停用' : '启用' }}
              </el-button>
              <el-button
                v-if="row.id !== auth.user?.id"
                link
                @click="change(row, row.enabled, row.role === 'ADMIN' ? 'SUPER_ADMIN' : 'ADMIN')"
              >
                {{ row.role === 'ADMIN' ? '设为超级管理员' : '设为管理员' }}
              </el-button>
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
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @change="load"
    />

    <el-dialog v-model="dialog" title="创建账号" width="min(92vw,480px)">
      <form class="nx-stack-form" @submit.prevent="create">
        <label>
          用户名
          <input
            v-model="form.username"
            required
            pattern="[a-zA-Z0-9_.-]{3,64}"
            autocomplete="off"
            placeholder="3–64 位字母、数字、下划线、点或短横线"
          />
        </label>
        <label>
          初始密码
          <input
            v-model="form.password"
            required
            type="password"
            minlength="12"
            maxlength="72"
            autocomplete="new-password"
          />
        </label>
        <label>
          角色
          <select v-model="form.role">
            <option value="ADMIN">管理员</option>
            <option value="SUPER_ADMIN">超级管理员</option>
          </select>
        </label>
        <button class="nx-btn nx-btn--primary" :disabled="busy">创建</button>
      </form>
    </el-dialog>
  </section>
</template>
