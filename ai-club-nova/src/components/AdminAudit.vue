<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, message } from '../api'
import PaginationBar from './PaginationBar.vue'

const items = ref<Record<string, unknown>[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const busy = ref(false)
const error = ref('')

let requestId = 0

function field(row: Record<string, unknown>, key: string) {
  return row[key] ?? row[key.toUpperCase()] ?? ''
}

function time(row: Record<string, unknown>) {
  const raw = String(field(row, 'createdAt') || '')
  return raw ? raw.replace('T', ' ').slice(0, 19) : ''
}

async function load() {
  const currentRequest = ++requestId
  busy.value = true
  error.value = ''
  try {
    const { data } = await api.get('/admin/audit', {
      params: { page: page.value, size: pageSize.value },
    })
    if (currentRequest !== requestId) return
    items.value = data.items
    total.value = data.total
  } catch (e) {
    if (currentRequest === requestId) error.value = message(e)
  } finally {
    if (currentRequest === requestId) busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="nx-admin__panel">
    <div class="nx-admin__panel-head">
      <div>
        <p class="nx-kicker">ACTIVITY LOG</p>
        <h2>操作记录</h2>
      </div>
      <span class="nx-admin__count"><b>{{ total }}</b> 条记录</span>
    </div>
    <p class="nx-admin__hint">
      记录内容修改、发布、下架、图片上传和账号权限变更，便于交接与追溯。
    </p>
    <p v-if="error" class="nx-notice nx-notice--error">
      <span>{{ error }}</span>
      <button @click="load">重试</button>
    </p>

    <div class="nx-admin__table">
      <el-table :data="items" v-loading="busy">
        <el-table-column label="操作人" min-width="160">
          <template #default="{ row }">{{ field(row, 'actor') }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <span class="nx-audit-action">{{ field(row, 'action') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="120">
          <template #default="{ row }">{{ field(row, 'target') }}</template>
        </el-table-column>
        <el-table-column label="时间" min-width="200">
          <template #default="{ row }">{{ time(row) }}</template>
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
  </section>
</template>
