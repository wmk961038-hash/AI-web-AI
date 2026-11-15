<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { DomEditor, type IDomEditor, type IEditorConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, message, meta, type Content, type Meta } from '../api'
import { sectionByKey } from '../sections'

const route = useRoute()
const router = useRouter()
const editor = shallowRef<IDomEditor>()
const page = ref<Content>()
const fields = ref<Meta>({})
const ready = ref(false)
const loading = ref(true)
const busy = ref(false)
const dirty = ref(false)
const error = ref('')

const pageKey = computed(() => String(route.params.key))
const section = computed(() => sectionByKey(pageKey.value))
const isJoin = computed(() => pageKey.value === 'join')
const statusText = computed(() =>
  page.value?.status === 'PUBLISHED' ? '已发布' : '草稿',
)

const form = ref({
  title: '',
  summary: '',
  body: '<p>待补充</p>',
  version: 0,
})

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '在这里编写页面正文…',
  MENU_CONF: {
    uploadImage: {
      customUpload: async (
        file: File,
        insert: (url: string, alt: string, href: string) => void,
      ) => {
        try {
          const result = await upload(file)
          insert(result.url, file.name, '')
        } catch (e) {
          ElMessage.error(message(e))
        }
      },
    },
  },
}

async function upload(file: File) {
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 10 * 1024 * 1024)
    throw new Error('仅支持 10MB 以内的 JPEG/PNG 图片')
  const data = new FormData()
  data.append('file', file)
  return (await api.post('/admin/media', data)).data
}

async function load() {
  if (!section.value || section.value.kind !== 'page') {
    await router.replace('/admin')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/admin/contents', {
      params: { type: 'PAGE', size: 100 },
    })
    const found = (data.items as Content[]).find((item) => item.category === pageKey.value)
    if (!found) throw new Error('页面内容不存在，请检查当前内容维护状态')
    page.value = found
    form.value = {
      title: found.title,
      summary: found.summary,
      body: found.body,
      version: found.version,
    }
    fields.value = meta(found)
    ready.value = true
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!page.value || busy.value) return false
  if (!form.value.title.trim()) {
    ElMessage.error('请填写页面标题')
    return false
  }
  busy.value = true
  try {
    const { data } = await api.put(`/admin/contents/${page.value.id}`, {
      type: page.value.type,
      title: form.value.title.trim(),
      category: page.value.category,
      summary: form.value.summary,
      body: form.value.body,
      coverId: page.value.coverId,
      metadata: fields.value,
      sortOrder: page.value.sortOrder,
      featured: page.value.featured,
      pinned: page.value.pinned,
      archived: page.value.archived,
      version: form.value.version,
    })
    page.value = data
    form.value.version = data.version
    dirty.value = false
    ElMessage.success(
      data.status === 'PUBLISHED' ? '页面已保存，官网内容已更新' : '页面草稿已保存',
    )
    return true
  } catch (e) {
    ElMessage.error(message(e))
    return false
  } finally {
    busy.value = false
  }
}

async function publish() {
  if (!page.value) return
  if (dirty.value && !(await save())) return
  if (!page.value) return
  busy.value = true
  try {
    const { data } = await api.post(`/admin/contents/${page.value.id}/publish`, {
      version: form.value.version,
    })
    page.value = data
    form.value.version = data.version
    ElMessage.success('页面已发布')
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

async function preview() {
  if (!page.value) return
  if (dirty.value && !(await save())) return
  await router.push(`/admin/preview/${page.value.id}`)
}

onBeforeUnmount(() => {
  const instance = editor.value
  if (!instance) return
  const area = DomEditor.getTextarea(instance) as unknown as {
    onDOMSelectionChange?: { cancel?: () => void }
  }
  area.onDOMSelectionChange?.cancel?.()
  instance.destroy()
})

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('有尚未保存的修改，确定离开？', '未保存的内容', {
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
})

function unload(e: BeforeUnloadEvent) {
  if (dirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(async () => {
  window.addEventListener('beforeunload', unload)
  await load()
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', unload))
</script>

<template>
  <div class="nx-page-editor">
    <header class="nx-editor__top">
      <div class="nx-editor__top-left">
        <RouterLink class="nx-back" to="/admin"><i>←</i> 返回内容管理</RouterLink>
        <div class="nx-editor__heading">
          <p class="nx-kicker">PAGE EDITOR / {{ statusText }}</p>
          <h1>{{ section?.title || '编辑页面' }}</h1>
        </div>
      </div>
      <div class="nx-editor__top-actions">
        <span
          class="nx-editor__status"
          :class="`is-${(page?.status || 'DRAFT').toLowerCase()}`"
        >
          <i></i>{{ statusText }}
        </span>
        <el-button :disabled="busy" @click="save">保存修改</el-button>
        <el-button :disabled="busy" @click="preview">保存并预览</el-button>
        <el-button
          v-if="page?.status !== 'PUBLISHED'"
          type="primary"
          :disabled="busy"
          @click="publish"
        >
          发布
        </el-button>
      </div>
    </header>

    <p v-if="loading" class="nx-editor__loading">正在加载页面内容…</p>

    <p v-if="error" class="nx-notice nx-notice--error" role="alert">
      <span>{{ error }}</span>
      <button @click="load">重试</button>
    </p>

    <form
      v-if="ready && page"
      class="nx-page-editor__body"
      @submit.prevent="save"
      @input="dirty = true"
      @change="dirty = true"
    >
      <section class="nx-panel">
        <div class="nx-panel__head">
          <span>01</span>
          <div>
            <p class="nx-kicker">PAGE CONTENT</p>
            <h2>页面正文</h2>
          </div>
          <small class="nx-page-editor__key">{{ section?.english }} / {{ pageKey }}</small>
        </div>

        <label class="nx-form-row">
          <span>页面标题</span>
          <input v-model="form.title" required maxlength="180" placeholder="输入页面标题" />
        </label>

        <label class="nx-form-row">
          <span>页面摘要</span>
          <textarea
            v-model="form.summary"
            maxlength="600"
            rows="3"
            placeholder="显示在页面开头的简介"
          ></textarea>
        </label>

        <div class="nx-form-row">
          <span>正文</span>
          <div class="nx-rich">
            <Toolbar
              :editor="editor"
              :default-config="{
                excludeKeys: [
                  'group-video',
                  'fullScreen',
                  'insertImage',
                  'editImage',
                  'uploadVideo',
                  'insertVideo',
                ],
              }"
            />
            <Editor
              v-model="form.body"
              :default-config="editorConfig"
              @on-created="(e: IDomEditor) => (editor = e)"
              @on-change="
                () => {
                  if (ready) dirty = true
                }
              "
              style="height: 520px; overflow-y: auto"
            />
          </div>
        </div>

        <div class="nx-page-editor__fields">
          <label class="nx-form-row">
            <span>资料来源</span>
            <input v-model="fields.sourceName" maxlength="200" placeholder="如通信学院公众号" />
          </label>
          <label class="nx-form-row">
            <span>原文链接</span>
            <input v-model="fields.sourceUrl" type="url" placeholder="https://…" />
          </label>
          <label class="nx-form-row">
            <span>图片来源与署名</span>
            <input v-model="fields.credit" maxlength="500" />
          </label>
          <label v-if="isJoin" class="nx-form-row">
            <span>外部报名链接</span>
            <input v-model="fields.joinUrl" type="url" placeholder="https://…" />
          </label>
        </div>
      </section>

      <div class="nx-page-editor__footer">
        <p>当前页面不提供相册与发布设置；封面、排序、推荐、置顶与归档状态保持原值。</p>
        <button class="nx-btn nx-btn--primary" :disabled="busy">
          <span>{{ busy ? '正在保存…' : '保存页面内容' }}</span>
          <i aria-hidden="true">→</i>
        </button>
      </div>
    </form>
  </div>
</template>
