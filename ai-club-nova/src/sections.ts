export type SectionKind = 'page' | 'collection'

export interface SectionDef {
  key: string
  title: string
  english: string
  desc: string
  kind: SectionKind
  pageKey?: string
  types: string[]
  editPath: string
  previewPath: string
  addPath?: string
  categories?: string[]
}

export const sections: SectionDef[] = [
  {
    key: 'about',
    title: '关于我们',
    english: 'ABOUT THE CLUB',
    desc: '社团介绍、竞赛科研方向、组织架构与联系方式。',
    kind: 'page',
    pageKey: 'about',
    types: ['PAGE'],
    editPath: '/admin/pages/about',
    previewPath: '/about',
  },
  {
    key: 'news',
    title: '新闻与活动',
    english: 'NEWS & EVENTS',
    desc: '新闻动态、活动预告与活动回顾，记录社团的每一次发生。',
    kind: 'collection',
    types: ['NEWS', 'ACTIVITY'],
    editPath: '/admin/section/news',
    previewPath: '/news',
    addPath: '/admin/edit/new?section=news',
    categories: ['新闻动态', '活动预告', '活动回顾'],
  },
  {
    key: 'projects',
    title: '项目与成果',
    english: 'PROJECTS & ACHIEVEMENTS',
    desc: '技术项目、竞赛获奖与科研成果，展示把想法做成真的过程。',
    kind: 'collection',
    types: ['PROJECT'],
    editPath: '/admin/section/projects',
    previewPath: '/projects',
    addPath: '/admin/edit/new?section=projects',
    categories: ['技术项目', '竞赛获奖', '科研成果'],
  },
  {
    key: 'people',
    title: '师生风采',
    english: 'PEOPLE & COMMUNITY',
    desc: '指导老师、社团骨干与成员，因为共同的热爱在这里相遇。',
    kind: 'collection',
    types: ['PERSON'],
    editPath: '/admin/section/people',
    previewPath: '/people',
    addPath: '/admin/people/new',
    categories: ['指导老师', '社团骨干', '成员'],
  },
  {
    key: 'join',
    title: '加入我们',
    english: 'JOIN OUR COMMUNITY',
    desc: '招新对象、技术方向、加入方式、常见问题与报名入口。',
    kind: 'page',
    pageKey: 'join',
    types: ['PAGE'],
    editPath: '/admin/pages/join',
    previewPath: '/join',
  },
]

export const collectionSections = sections.filter((section) => section.kind === 'collection')

export function sectionByKey(key: string) {
  return sections.find((section) => section.key === key)
}

export function sectionOfType(type: string) {
  return collectionSections.find((section) => section.types.includes(type))
}

export function sectionBackPath(type: string) {
  const section = sectionOfType(type)
  return section ? section.editPath : '/admin'
}

export function allowedTypes(sectionKey: string) {
  return sectionByKey(sectionKey)?.types || []
}

export function defaultCategory(type: string) {
  if (type === 'ACTIVITY') return '活动预告'
  if (type === 'PROJECT') return '技术项目'
  if (type === 'PERSON') return '成员'
  return '新闻动态'
}

export function categoriesFor(type: string) {
  if (type === 'PROJECT') return ['技术项目', '竞赛获奖', '科研成果']
  if (type === 'ACTIVITY') return ['活动预告', '活动回顾']
  if (type === 'PERSON') return ['指导老师', '社团骨干', '成员']
  return ['新闻动态']
}
