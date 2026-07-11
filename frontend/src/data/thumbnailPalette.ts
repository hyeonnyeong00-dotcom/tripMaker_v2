// frontend/src/data/thumbnailPalette.ts
// 목적지 문자열 해시 → 여행 카드 썸네일 팔레트. destinations.ts와 마찬가지로
// 목적지별 값이 동적으로 달라져야 하므로 §7 "컴포넌트에 색 하드코딩 금지" 예외로 데이터 파일에 분리.

export interface ThumbnailTheme {
  bg: string
  fg: string
}

const PALETTE: ThumbnailTheme[] = [
  { bg: '#E6F0FE', fg: '#0062F4' },
  { bg: '#FFF1E6', fg: '#C2571A' },
  { bg: '#E9F7EF', fg: '#1D7A46' },
  { bg: '#FCEAF5', fg: '#B23A82' },
  { bg: '#F2ECFB', fg: '#6A3FC7' },
  { bg: '#FFF7E0', fg: '#9A6700' },
  { bg: '#E7F5F7', fg: '#12767E' },
  { bg: '#FDEDED', fg: '#C23B3A' },
]

function hashString(value: string): number {
  let hash = 5381
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 33) ^ value.charCodeAt(i)
  }
  return hash >>> 0
}

export function getDestinationTheme(destination: string): ThumbnailTheme {
  const normalized = destination.trim()
  if (!normalized) return PALETTE[0]
  return PALETTE[hashString(normalized) % PALETTE.length]
}
