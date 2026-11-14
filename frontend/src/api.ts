import axios from 'axios'
export const api = axios.create({ baseURL: '/api', timeout: 20000, withCredentials: true })
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && window.location.pathname.startsWith('/admin')) {
      window.location.assign('/login?redirect=' + encodeURIComponent(window.location.pathname))
    }
    return Promise.reject(error)
  },
)
let csrf: { token: string; headerName: string } | null = null
export async function refreshCsrf() {
  csrf = (await api.get('/auth/csrf')).data
}
api.interceptors.request.use(async (config) => {
  if (!['get', 'head', 'options'].includes(config.method || 'get')) {
    if (!csrf) await refreshCsrf()
    config.headers.set(csrf!.headerName, csrf!.token)
  }
  return config
})
export function message(e: unknown) {
  return axios.isAxiosError(e)
    ? e.response?.data?.message || '网络连接失败，请稍后重试'
    : e instanceof Error
      ? e.message
      : '操作失败'
}
export interface Account {
  id: number
  username: string
  role: 'ADMIN' | 'SUPER_ADMIN'
  enabled: boolean
}
export interface Meta {
  startAt?: string
  endAt?: string
  location?: string
  activityStatus?: string
  members?: string
  repository?: string
  demo?: string
  sourceName?: string
  sourceUrl?: string
  credit?: string
  cohort?: string
  position?: string
  gallery?: number[]
  joinUrl?: string
}
export interface Content {
  createdBy?: string
  id: number
  type: string
  title: string
  category: string
  summary: string
  body: string
  coverId?: number
  metadata: string
  status: string
  sortOrder: number
  featured: boolean
  pinned: boolean
  archived: boolean
  version: number
  updatedAt?: string
  publishedAt?: string
}
export interface Page<T> {
  items: T[]
  total: number
  page: number
  size: number
}
export const labels: Record<string, string> = {
  NEWS: '新闻动态',
  ACTIVITY: '社团活动',
  PROJECT: '项目与成果',
  PERSON: '师生风采',
  PAGE: '页面设置',
}
export const states: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  TRASH: '回收站',
}
export function meta(c: Content): Meta {
  try {
    return JSON.parse(c.metadata)
  } catch {
    return {}
  }
}
export function activityState(c: Content) {
  const m = meta(c)
  return m.endAt && new Date(m.endAt) < new Date() ? '已结束' : m.activityStatus || '待补充'
}
export function date(s?: string) {
  return s ? s.slice(0, 10) : '待补充'
}
