// frontend/src/data/tripFormOptions.ts
// 일정 생성 폼 목업용 정적 옵션 (예산 빠른 입력 / 취향 칩 / 로딩 체크리스트 단계)

export const BUDGET_QUICK_OPTIONS: string[] = ['알뜰하게', '적당히', '플렉스']

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

export const GENERATION_STEPS: string[] = ['취향 분석', '장소 후보 탐색', '동선 계산', '이동 시간 계산']
