<script setup lang="ts">
withDefaults(
  defineProps<{
    page: number
    pageSize: number
    total: number
    pageSizes?: number[]
    layout?: string
  }>(),
  {
    pageSizes: () => [20, 50, 100],
    layout: 'prev, pager, next, total',
  },
)

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:pageSize', value: number): void
  (e: 'change'): void
}>()

function onPage(value: number) {
  emit('update:page', value)
  emit('change')
}

function onSize(value: number) {
  emit('update:pageSize', value)
  emit('update:page', 1)
  emit('change')
}
</script>

<template>
  <div v-if="total > 0" class="nx-pagination">
    <el-pagination
      :current-page="page"
      :page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      :layout="layout"
      background
      @current-change="onPage"
      @size-change="onSize"
    />
  </div>
</template>
