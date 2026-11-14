<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { DomEditor, type IDomEditor, type IEditorConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, labels, message, meta, type Content, type Meta } from '../api'

const route = useRoute()
const router = useRouter()
const editor = shallowRef<IDomEditor>()
const busy = ref(false)
const ready = ref(false)
const dirty = ref(false)
const loading = ref(true)
const loadError = ref('')

const form = ref({
  type: 'NEWS',
  title: '',
  category: '新闻动态',
  summary: '',
  body: '<p>待补充</p>',
  coverId: undefined as number | undefined,
  sortOrder: 0,
  featured: false,
  pinned: false,
  archived: false,
  version: 0,
})
const fields = ref<Meta>({ gallery: [] })
const id = ref<number>()
const status = ref('DRAFT')

const isNew = computed(() => !id.value)
const categories = computed(() =>
  form.value.type === 'PROJECT'
    ? ['技术项目', '竞赛获奖', '科研成果']
    : form.value.type === 'ACTIVITY'
      ? ['活动预告', '活动回顾']
      : form.value.type === 'PERSON'
        ? ['指导老师', '社团骨干', '成员']
        : ['新闻动态'],
)

const statusText = computed(() =>
  status.value === 'PUBLISHED' ? '已发布' : status.value === 'TRASH' ? '回收站' : '草稿',
)

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '在这里编写正文…',
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

function removeGallery(index: number) {
  fields.value.gallery?.splice(index, 1)
  dirty.value = true
}

function removeCover() {
  form.value.coverId = undefined
  dirty.value = true
}

async function upload(file: File) {
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 10 * 1024 * 1024)
    throw new Error('仅支持 10MB 以内的 JPEG/PNG 图片')
  const data = new FormData()
  data.append('file', file)
  return (await api.post('/admin/media', data)).data
}

async function choose(e: Event, kind: string) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  busy.value = true
  try {
    for (const file of Array.from(input.files)) {
      const result = await upload(file)
      if (kind === 'cover') form.value.coverId = result.id
      else fields.value.gallery = [...(fields.value.gallery || []), result.id]
    }
    dirty.value = true
    ElMessage.success('图片已上传，保存内容后建立引用')
  } catch (err) {
    ElMessage.error(message(err))
  } finally {
    busy.value = false
    input.value = ''
  }
}

async function save(navigate = true) {
  if (busy.value) return false
  busy.value = true
  try {
    const payload = { ...form.value, metadata: fields.value }
    const { data } = id.value
      ? await api.put('/admin/contents/' + id.value, payload)
      : await api.post('/admin/contents', payload)
    id.value = data.id
    form.value.version = data.version
    status.value = data.status
    dirty.value = false
    ElMessage.success(status.value === 'PUBLISHED' ? '已保存，官网内容已更新' : '草稿已保存')
    if (navigate && route.params.id === 'new') await router.replace('/admin/edit/' + id.value)
    return true
  } catch (e) {
    ElMessage.error(message(e))
    return false
  } finally {
    busy.value = false
  }
}

async function publish() {
  if (!(await save(false))) return
  busy.value = true
  try {
    const { data } = await api.post('/admin/contents/' + id.value + '/publish', {
      version: form.value.version,
    })
    status.value = data.status
    form.value.version = data.version
    ElMessage.success('内容已正式发布')
    if (route.params.id === 'new') await router.replace('/admin/edit/' + id.value)
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

async function preview() {
  if (dirty.value || !id.value) {
    if (!(await save(false))) return
  }
  await router.push('/admin/preview/' + id.value)
}

onMounted(async () => {
  try {
    if (route.params.id !== 'new') {
      const { data } = await api.get<Content>('/admin/contents/' + route.params.id)
      const {
        type,
        title,
        category,
        summary,
        body,
        coverId,
        sortOrder,
        featured,
        pinned,
        archived,
        version,
      } = data
      form.value = {
        type,
        title,
        category,
        summary,
        body,
        coverId,
        sortOrder,
        featured,
        pinned,
        archived,
        version,
      }
      fields.value = { ...meta(data), gallery: meta(data).gallery || [] }
      id.value = data.id
      status.value = data.status
    }
    ready.value = true
  } catch (e) {
    loadError.value = message(e)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  const instance = editor.value
  if (!instance) return
  // wangEditor 5 removes the listener but leaves its trailing selection timer alive.
  // Cancel it before destroy so route changes cannot call a disposed editor.
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
window.addEventListener('beforeunload', unload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', unload))
</script>

<template>
  <div class="nx-editor">
    <header class="nx-editor__top">
      <div class="nx-editor__top-left">
        <RouterLink class="nx-back" to="/admin"><i>←</i> 返回内容管理</RouterLink>
        <div class="nx-editor__heading">
          <p class="nx-kicker">CONTENT EDITOR / {{ statusText }}</p>
          <h1>{{ isNew ? '新建内容' : '编辑内容' }}</h1>
        </div>
      </div>
      <div class="nx-editor__top-actions">
        <span class="nx-editor__status" :class="`is-${status.toLowerCase()}`">
          <i></i>{{ statusText }}
        </span>
        <el-button :disabled="busy" @click="preview">保存并预览</el-button>
        <el-button type="primary" :disabled="busy" @click="publish">保存并发布</el-button>
      </div>
    </header>

    <p v-if="loading" class="nx-editor__loading">正在加载编辑器…</p>
    <p v-if="loadError" class="nx-notice nx-notice--error" role="alert">
      <span>{{ loadError }}</span>
    </p>

    <template v-if="ready">
      <form
        class="nx-editor__layout"
        @submit.prevent="save()"
        @input="dirty = true"
        @change="dirty = true"
      >
        <div class="nx-editor__main">
          <section class="nx-panel">
            <div class="nx-panel__head">
              <span>01</span>
              <div>
                <p class="nx-kicker">MAIN CONTENT</p>
                <h2>正文内容</h2>
              </div>
            </div>

            <label class="nx-form-row">
              <span>标题</span>
              <input
                v-model="form.title"
                required
                maxlength="180"
                placeholder="输入一个清晰的标题"
              />
            </label>

            <label class="nx-form-row">
              <span>摘要</span>
              <textarea
                v-model="form.summary"
                maxlength="600"
                rows="3"
                placeholder="简要介绍内容，显示在列表卡片中"
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
                  style="height: 460px; overflow-y: auto"
                />
              </div>
            </div>

            <div class="nx-editor__fields">
              <label v-if="form.type === 'ACTIVITY'" class="nx-form-row">
                <span>开始时间</span><input v-model="fields.startAt" type="datetime-local" />
              </label>
              <label v-if="form.type === 'ACTIVITY'" class="nx-form-row">
                <span>结束时间</span><input v-model="fields.endAt" type="datetime-local" />
              </label>
              <label v-if="form.type === 'ACTIVITY'" class="nx-form-row">
                <span>活动地点</span><input v-model="fields.location" maxlength="200" />
              </label>
              <label v-if="form.type === 'ACTIVITY'" class="nx-form-row">
                <span>活动状态</span>
                <select v-model="fields.activityStatus">
                  <option>待补充</option>
                  <option>即将开始</option>
                  <option>进行中</option>
                  <option>已结束</option>
                  <option>已取消</option>
                </select>
              </label>
              <label v-if="form.type === 'PROJECT'" class="nx-form-row">
                <span>参与成员</span><input v-model="fields.members" maxlength="1000" />
              </label>
              <label v-if="form.type === 'PROJECT'" class="nx-form-row">
                <span>代码仓库</span>
                <input v-model="fields.repository" type="url" placeholder="https://…" />
              </label>
              <label v-if="form.type === 'PROJECT'" class="nx-form-row">
                <span>演示链接</span>
                <input v-model="fields.demo" type="url" placeholder="https://…" />
              </label>
              <label v-if="form.type === 'PERSON'" class="nx-form-row">
                <span>届别</span>
                <input v-model="fields.cohort" maxlength="40" placeholder="如 2026" />
              </label>
              <label v-if="form.type === 'PERSON'" class="nx-form-row">
                <span>身份 / 职务</span><input v-model="fields.position" maxlength="100" />
              </label>
              <label class="nx-form-row">
                <span>资料来源</span>
                <input
                  v-model="fields.sourceName"
                  maxlength="200"
                  placeholder="如通信学院公众号"
                />
              </label>
              <label class="nx-form-row">
                <span>原文链接</span>
                <input v-model="fields.sourceUrl" type="url" placeholder="https://…" />
              </label>
              <label class="nx-form-row">
                <span>图片来源与署名</span><input v-model="fields.credit" maxlength="500" />
              </label>
              <label v-if="form.type === 'PAGE' && form.category === 'join'" class="nx-form-row">
                <span>外部报名链接</span>
                <input v-model="fields.joinUrl" type="url" placeholder="https://…" />
              </label>
            </div>
          </section>

          <section class="nx-panel">
            <div class="nx-panel__head">
              <span>02</span>
              <div>
                <p class="nx-kicker">GALLERY</p>
                <h2>活动照片 / 相册</h2>
              </div>
            </div>
            <label class="nx-file">
              <input
                type="file"
                accept="image/jpeg,image/png"
                multiple
                :disabled="busy"
                @change="choose($event, 'gallery')"
                aria-label="上传相册照片"
              />
              <span>＋ 选择照片</span>
              <small>JPEG / PNG，单张不超过 10MB</small>
            </label>
            <div v-if="fields.gallery?.length" class="nx-editor__gallery">
              <div v-for="(mediaId, index) in fields.gallery" :key="mediaId">
                <img :src="`/api/media/${mediaId}`" :alt="`照片 ${index + 1}`" />
                <button type="button" @click="removeGallery(index)">移除</button>
              </div>
            </div>
          </section>
        </div>

        <aside class="nx-editor__side">
          <section class="nx-panel nx-editor__settings">
            <div class="nx-panel__head">
              <span>03</span>
              <div>
                <p class="nx-kicker">PUBLISH SETTINGS</p>
                <h2>发布设置</h2>
              </div>
            </div>

            <label class="nx-form-row">
              <span>内容类型</span>
              <select
                v-model="form.type"
                :disabled="!isNew"
                @change="form.category = categories[0] || ''"
              >
                <option
                  v-for="(name, key) in labels"
                  v-show="key !== 'PAGE' || !isNew"
                  :value="key"
                  :key="key"
                >
                  {{ name }}
                </option>
              </select>
            </label>

            <label class="nx-form-row">
              <span>分类</span>
              <input v-if="form.type === 'PAGE'" :value="form.category" disabled />
              <select v-else v-model="form.category">
                <option v-for="c in categories" :key="c">{{ c }}</option>
              </select>
            </label>

            <label class="nx-form-row">
              <span>排序<small>数值越小越靠前</small></span>
              <input v-model.number="form.sortOrder" type="number" min="-100000" max="100000" />
            </label>

            <label class="nx-check">
              <input v-model="form.featured" type="checkbox" />
              <span><b>首页推荐项目</b><small>展示在首页重点项目区域</small></span>
            </label>
            <label class="nx-check">
              <input v-model="form.pinned" type="checkbox" />
              <span><b>置顶动态</b><small>优先展示在新闻动态列表</small></span>
            </label>
            <label v-if="form.type === 'PERSON'" class="nx-check">
              <input v-model="form.archived" type="checkbox" />
              <span><b>按届别归档</b><small>归档后仍可公开浏览</small></span>
            </label>

            <div class="nx-editor__cover">
              <h3>
                {{ form.type === 'PAGE' && form.category === 'home' ? '首页精选图片' : '封面图片' }}
              </h3>
              <div v-if="form.coverId" class="nx-editor__cover-preview">
                <img :src="`/api/media/${form.coverId}`" alt="封面预览" />
                <button type="button" @click="removeCover">移除封面</button>
              </div>
              <label class="nx-file">
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  :disabled="busy"
                  @change="choose($event, 'cover')"
                  aria-label="上传封面"
                />
                <span>＋ 上传封面</span>
                <small>JPEG / PNG，最大 10MB，自动压缩</small>
              </label>
            </div>

            <p class="nx-admin__hint">
              图片会自动压缩。请确认照片与个人资料已经获得公开授权。
            </p>

            <button class="nx-btn nx-btn--primary nx-editor__save" :disabled="busy">
              <span>
                {{ busy ? '正在保存…' : status === 'PUBLISHED' ? '保存修改' : '保存草稿' }}
              </span>
              <i aria-hidden="true">→</i>
            </button>

            <p v-if="status === 'PUBLISHED'" class="nx-admin__hint">
              当前内容已发布，保存修改后立即更新官网；需要暂存修改时请先下架。
            </p>
          </section>
        </aside>
      </form>
    </template>
  </div>
</template>
