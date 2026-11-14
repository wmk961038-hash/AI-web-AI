<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, message } from '../api'
const items = ref<Record<string, unknown>[]>([]),
  page = ref(1),
  total = ref(0),
  busy = ref(false),
  error = ref('')
async function load() {
  busy.value = true
  error.value = ''
  try {
    const { data } = await api.get('/admin/audit', { params: { page: page.value, size: 20 } })
    items.value = data.items
    total.value = data.total
  } catch (e) {
    error.value = message(e)
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <section>
    <h2>操作记录</h2>
    <p class="admin-hint">记录内容修改、发布、下架、图片上传和账号权限变更，便于交接与追溯。</p>
    <p v-if="error" class="notice error">{{ error }} <button @click="load">重试</button></p>
    <div class="table-scroll">
      <el-table :data="items" v-loading="busy"
        ><el-table-column prop="actor" label="操作人" min-width="140" /><el-table-column
          prop="action"
          label="操作"
          min-width="180" /><el-table-column
          prop="target"
          label="对象"
          min-width="100" /><el-table-column prop="createdAt" label="时间" min-width="180"
      /></el-table>
    </div>
    <el-pagination
      v-if="total > 20"
      v-model:current-page="page"
      :total="total"
      :page-size="20"
      layout="prev, pager, next, total"
      @current-change="load"
    />
  </section>
</template>
