<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { DomEditor, type IDomEditor, type IEditorConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, message, meta, personRoleOf, type Content } from '../api'

const route = useRoute()
const router = useRouter()
const editor = shallowRef<IDomEditor>()
const item = ref<Content>()
const ready = ref(false)
const loading = ref(true)
const busy = ref(false)
const dirty = ref(false)
const error = ref('')
const status = ref('DRAFT')

const isNew = computed(() => route.params.id === 'new')
const statusText = computed(() =>
  status.value === 'PUBLISHED' ? '已发布' : status.value === 'TRASH' ? '回收站' : '草稿',
)

const form = ref({
  name: '',
  category: '',
  personRole: '',
  coverId: undefined as number | undefined,
  bio: '<p></p>',
  version: 0,
})

const categorySuggestions = ['指导老师', '社团骨干', '成员']

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '填写人物简介…',
  MENU_CONF: {},
}

async function upload(file: File) {
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 10 * 1024 * 1024)
    throw new Error('仅支持 10MB 以内的 JPEG/PNG 图片')
  const data = new FormData()
  data.append('file', file)
  return (await api.post('/admin/media', data)).data
}

async function chooseImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  busy.value = true
  try {
    const result = await upload(file)
    form.value.coverId = result.id
    dirty.value = true
    ElMessage.success('图片已上传，保存后建立引用')
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
    input.value = ''
  }
}

function removeImage() {
  form.value.coverId = undefined
  dirty.value = true
}

async function load() {
  if (isNew.value) {
    ready.value = true
    loading.value = false
    return
  }
  try {
    const { data } = await api.get<Content>(`/admin/contents/${route.params.id}`)
    if (data.type !== 'PERSON') {
      await router.replace(`/admin/edit/${data.id}`)
      return
    }
    item.value = data
    form.value = {
      name: data.title,
      category: data.category,
      personRole: personRoleOf(data),
      coverId: data.coverId,
      bio: data.body || '<p></p>',
      version: data.version,
    }
    status.value = data.status
    ready.value = true
  } catch (e) {
    error.value = message(e)
  } finally {
    loading.value = false
  }
}

async function save(navigate = true) {
  if (busy.value) return false
  if (!form.value.name.trim()) {
    ElMessage.error('请填写姓名')
    return false
  }
  if (!form.value.category.trim()) {
    ElMessage.error('请填写类别')
    return false
  }
  if (form.value.category.includes(',')) {
    ElMessage.error('类别不能包含英文逗号')
    return false
  }
  if (!form.value.personRole.trim()) {
    ElMessage.error('请填写职务方向')
    return false
  }
  if (!form.value.coverId) {
    ElMessage.error('请上传相关图片')
    return false
  }
  if (!form.value.bio.replace(/<[^>]*>/g, '').trim()) {
    ElMessage.error('请填写简介')
    return false
  }

  busy.value = true
  try {
    const base = item.value
    const payload = {
      type: 'PERSON',
      title: form.value.name.trim(),
      category: form.value.category.trim(),
      personRole: form.value.personRole.trim(),
      summary: base?.summary ?? '',
      body: form.value.bio,
      coverId: form.value.coverId,
      metadata: base ? meta(base) : {},
      sortOrder: base?.sortOrder ?? 0,
      featured: base?.featured ?? false,
      pinned: base?.pinned ?? false,
      archived: base?.archived ?? false,
      version: form.value.version,
    }
    const { data } = isNew.value
      ? await api.post('/admin/contents', payload)
      : await api.put(`/admin/contents/${item.value!.id}`, payload)
    item.value = data
    form.value.version = data.version
    status.value = data.status
    dirty.value = false
    ElMessage.success(
      data.status === 'PUBLISHED' ? '已保存，官网人物信息已更新' : '人物草稿已保存',
    )
    if (navigate && isNew.value) await router.replace(`/admin/people/${data.id}`)
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
    const { data } = await api.post(`/admin/contents/${item.value!.id}/publish`, {
      version: form.value.version,
    })
    item.value = data
    form.value.version = data.version
    status.value = data.status
    ElMessage.success('人物信息已发布')
    if (isNew.value) await router.replace(`/admin/people/${data.id}`)
  } catch (e) {
    ElMessage.error(message(e))
  } finally {
    busy.value = false
  }
}

async function preview() {
  if (dirty.value || isNew.value) {
    if (!(await save(false))) return
  }
  await router.push(`/admin/preview/${item.value!.id}`)
}

onMounted(load)

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
    await ElMessageBox.confirm('有尚未保存的修改，确定离开？', '未保存的人物信息', {
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
  <div class="nx-person-editor">
    <header class="nx-editor__top">
      <div class="nx-editor__top-left">
        <RouterLink class="nx-back" to="/admin/section/people"><i>←</i> 返回师生风采</RouterLink>
        <div class="nx-editor__heading">
          <p class="nx-kicker">PERSON EDITOR / {{ statusText }}</p>
          <h1>{{ isNew ? '新增师生' : '编辑师生' }}</h1>
        </div>
      </div>
      <div class="nx-editor__top-actions">
        <span class="nx-editor__status" :class="`is-${status.toLowerCase()}`">
          <i></i>{{ statusText }}
        </span>
        <el-button :disabled="busy" @click="save()">保存</el-button>
        <el-button :disabled="busy" @click="preview">保存并预览</el-button>
        <el-button
          v-if="status !== 'PUBLISHED'"
          type="primary"
          :disabled="busy"
          @click="publish"
        >
          发布
        </el-button>
      </div>
    </header>

    <p v-if="loading" class="nx-editor__loading">正在加载人物信息…</p>

    <p v-if="error" class="nx-notice nx-notice--error" role="alert">
      <span>{{ error }}</span>
      <button @click="load">重试</button>
    </p>

    <form
      v-if="ready"
      class="nx-page-editor__body"
      @submit.prevent="save()"
      @input="dirty = true"
      @change="dirty = true"
    >
      <section class="nx-panel">
        <div class="nx-panel__head">
          <span>01</span>
          <div>
            <p class="nx-kicker">PERSON PROFILE</p>
            <h2>人物信息</h2>
          </div>
          <small class="nx-page-editor__key">仅维护五个字段</small>
        </div>

        <div class="nx-person-editor__grid">
          <label class="nx-form-row">
            <span>姓名</span>
            <input v-model="form.name" required maxlength="180" placeholder="输入姓名" />
          </label>

          <label class="nx-form-row">
            <span>类别</span>
            <input
              v-model="form.category"
              required
              maxlength="80"
              list="person-category-options"
              placeholder="如 指导老师 / 社团骨干"
            />
            <datalist id="person-category-options">
              <option v-for="option in categorySuggestions" :key="option" :value="option"></option>
            </datalist>
          </label>
        </div>

        <label class="nx-form-row">
          <span>职务方向</span>
          <input
            v-model="form.personRole"
            required
            maxlength="100"
            placeholder="如 社长 / 计算机视觉方向"
          />
        </label>

        <div class="nx-form-row">
          <span>相关图片</span>
          <div class="nx-person-editor__image">
            <div v-if="form.coverId" class="nx-person-editor__preview">
              <img :src="`/api/media/${form.coverId}`" alt="相关图片预览" />
              <button type="button" @click="removeImage">移除图片</button>
            </div>
            <label class="nx-file">
              <input
                type="file"
                accept="image/jpeg,image/png"
                :disabled="busy"
                aria-label="上传相关图片"
                @change="chooseImage"
              />
              <span>＋ {{ form.coverId ? '更换图片' : '上传图片' }}</span>
              <small>JPEG / PNG，最大 10MB，单图</small>
            </label>
          </div>
        </div>

        <div class="nx-form-row">
          <span>简介</span>
          <div class="nx-rich">
            <Toolbar
              :editor="editor"
              :default-config="{
                excludeKeys: [
                  'group-video',
                  'group-image',
                  'fullScreen',
                  'insertImage',
                  'editImage',
                  'uploadImage',
                  'uploadVideo',
                  'insertVideo',
                ],
              }"
            />
            <Editor
              v-model="form.bio"
              :default-config="editorConfig"
              @on-created="(e: IDomEditor) => (editor = e)"
              @on-change="
                () => {
                  if (ready) dirty = true
                }
              "
              style="height: 360px; overflow-y: auto"
            />
          </div>
        </div>
      </section>

      <div class="nx-page-editor__footer">
        <p>只维护姓名、类别、职务方向、相关图片和简介；发布状态通过本页顶部或栏目列表管理。</p>
        <button class="nx-btn nx-btn--primary" :disabled="busy">
          <span>{{ busy ? '正在保存…' : status === 'PUBLISHED' ? '保存修改' : '保存草稿' }}</span>
          <i aria-hidden="true">→</i>
        </button>
      </div>
    </form>
  </div>
</template>
