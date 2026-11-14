<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { DomEditor, type IDomEditor, type IEditorConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, labels, message, meta, type Content, type Meta } from '../api'
const route = useRoute(),
  router = useRouter(),
  editor = shallowRef<IDomEditor>(),
  busy = ref(false),
  ready = ref(false),
  dirty = ref(false),
  loading = ref(true),
  loadError = ref('')
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
const fields = ref<Meta>({ gallery: [] }),
  id = ref<number>(),
  status = ref('DRAFT')
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
  <section class="editor-page">
    <RouterLink class="back-link" to="/admin">← 返回内容管理</RouterLink>
    <p v-if="loading">正在加载编辑器…</p>
    <p v-if="loadError" class="notice error" role="alert">{{ loadError }}</p>
    <template v-if="ready"
      ><div class="admin-top">
        <div>
          <p class="eyebrow">CONTENT EDITOR / {{ status }}</p>
          <h1>{{ isNew ? '新建内容' : '编辑内容' }}</h1>
        </div>
        <div class="editor-actions">
          <el-button :disabled="busy" @click="preview">保存并预览</el-button
          ><el-button type="primary" :disabled="busy" @click="publish">保存并发布</el-button>
        </div>
      </div>
      <form
        class="editor-layout"
        @submit.prevent="save()"
        @input="dirty = true"
        @change="dirty = true"
      >
        <div class="editor-content">
          <label
            >标题<input
              v-model="form.title"
              required
              maxlength="180"
              placeholder="输入一个清晰的标题" /></label
          ><label
            >摘要<textarea
              v-model="form.summary"
              maxlength="600"
              rows="3"
              placeholder="简要介绍内容，显示在列表卡片中"
            ></textarea></label
          ><label>正文</label>
          <div class="rich-editor">
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
            /><Editor
              v-model="form.body"
              :default-config="editorConfig"
              @on-created="(e: IDomEditor) => (editor = e)"
              @on-change="
                () => {
                  if (ready) dirty = true
                }
              "
              style="height: 430px; overflow-y: auto"
            />
          </div>
          <div class="editor-fields">
            <label v-if="form.type === 'ACTIVITY'"
              >开始时间<input v-model="fields.startAt" type="datetime-local" /></label
            ><label v-if="form.type === 'ACTIVITY'"
              >结束时间<input v-model="fields.endAt" type="datetime-local" /></label
            ><label v-if="form.type === 'ACTIVITY'"
              >活动地点<input v-model="fields.location" maxlength="200" /></label
            ><label v-if="form.type === 'ACTIVITY'"
              >活动状态<select v-model="fields.activityStatus">
                <option>待补充</option>
                <option>即将开始</option>
                <option>进行中</option>
                <option>已结束</option>
                <option>已取消</option>
              </select></label
            ><label v-if="form.type === 'PROJECT'"
              >参与成员<input v-model="fields.members" maxlength="1000" /></label
            ><label v-if="form.type === 'PROJECT'"
              >代码仓库<input
                v-model="fields.repository"
                type="url"
                placeholder="https://…" /></label
            ><label v-if="form.type === 'PROJECT'"
              >演示链接<input v-model="fields.demo" type="url" placeholder="https://…" /></label
            ><label v-if="form.type === 'PERSON'"
              >届别<input v-model="fields.cohort" maxlength="40" placeholder="如 2026" /></label
            ><label v-if="form.type === 'PERSON'"
              >身份 / 职务<input v-model="fields.position" maxlength="100" /></label
            ><label
              >资料来源<input
                v-model="fields.sourceName"
                maxlength="200"
                placeholder="如通信学院公众号" /></label
            ><label
              >原文链接<input
                v-model="fields.sourceUrl"
                type="url"
                placeholder="https://…" /></label
            ><label>图片来源与署名<input v-model="fields.credit" maxlength="500" /></label>
            <label v-if="form.type === 'PAGE' && form.category === 'join'"
              >外部报名链接<input v-model="fields.joinUrl" type="url" placeholder="https://…"
            /></label>
          </div>
          <h3>活动照片 / 相册</h3>
          <input
            type="file"
            accept="image/jpeg,image/png"
            multiple
            :disabled="busy"
            @change="choose($event, 'gallery')"
            aria-label="上传相册照片"
          />
          <div class="editor-gallery">
            <div v-for="(mediaId, index) in fields.gallery" :key="mediaId">
              <img :src="`/api/media/${mediaId}`" :alt="`照片 ${index + 1}`" /><button
                type="button"
                @click="removeGallery(index)"
              >
                移除
              </button>
            </div>
          </div>
        </div>
        <aside class="editor-settings">
          <h3>发布设置</h3>
          <label
            >内容类型<select
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
            </select></label
          ><label
            >分类<input v-if="form.type === 'PAGE'" :value="form.category" disabled /><select
              v-else
              v-model="form.category"
            >
              <option v-for="c in categories" :key="c">{{ c }}</option>
            </select></label
          >
          <label
            >排序（数值越小越靠前）<input
              v-model.number="form.sortOrder"
              type="number"
              min="-100000"
              max="100000"
          /></label>
          <label class="check"><input v-model="form.featured" type="checkbox" />首页推荐项目</label>
          <label class="check"><input v-model="form.pinned" type="checkbox" />置顶动态</label>
          <label v-if="form.type === 'PERSON'" class="check"
            ><input v-model="form.archived" type="checkbox" />按届别归档（仍可公开浏览）</label
          >
          <h3>
            {{ form.type === 'PAGE' && form.category === 'home' ? '首页精选图片' : '封面图片' }}
          </h3>
          <img
            v-if="form.coverId"
            class="cover-preview"
            :src="`/api/media/${form.coverId}`"
            alt="封面预览"
          /><input
            type="file"
            accept="image/jpeg,image/png"
            :disabled="busy"
            @change="choose($event, 'cover')"
            aria-label="上传封面"
          /><el-button v-if="form.coverId" link type="danger" @click="removeCover"
            >移除封面</el-button
          >
          <p class="admin-hint">
            JPEG / PNG，最大 10MB。图片会自动压缩。请确认照片与个人资料已经获得公开授权。
          </p>
          <button class="button primary" :disabled="busy">
            {{ busy ? '正在保存…' : status === 'PUBLISHED' ? '保存修改' : '保存草稿' }}
          </button>
          <p v-if="status === 'PUBLISHED'" class="admin-hint">
            当前内容已发布，保存修改后立即更新官网；需要暂存修改时请先下架。
          </p>
        </aside>
      </form></template
    >
  </section>
</template>
