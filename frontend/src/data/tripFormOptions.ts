// frontend/src/data/tripFormOptions.ts
// 일정 생성 폼 목업용 정적 옵션 (예산 슬라이더 / 누구와 칩 / 취향 칩 / 활동 시간대 / 로딩 체크리스트 단계)

export const BUDGET_MIN = 0
export const BUDGET_MAX = 500_000
export const BUDGET_STEP = 10_000

export interface CompanionOption {
  value: string
  label: string
}

export const COMPANION_OPTIONS: CompanionOption[] = [
  { value: 'solo', label: '혼자' },
  { value: 'couple', label: '연인과' },
  { value: 'parent', label: '가족과' },
  { value: 'friend', label: '친구와' },
  { value: 'kid', label: '아이와' },
  { value: 'etc', label: '기타' },
]

export const PREFERENCE_OPTIONS: string[] = [
  '먹방',
  '힐링',
  '액티비티',
  '문화·역사',
  '쇼핑',
  '자연',
  '카페',
  '사진 명소',
]

export interface ActiveTimePreset {
  label: string
  start: string
  end: string
}

export const ACTIVE_TIME_PRESETS: ActiveTimePreset[] = [
  { label: '아침형', start: '07:00', end: '19:00' },
  { label: '기본', start: '09:00', end: '21:00' },
  { label: '저녁형', start: '11:00', end: '23:00' },
]

export const DEFAULT_ACTIVE_START_TIME = '09:00'
export const DEFAULT_ACTIVE_END_TIME = '21:00'

export const GENERATION_STEPS: string[] = ['취향 분석', '장소 후보 탐색', '동선 계산', '이동 시간 계산']
