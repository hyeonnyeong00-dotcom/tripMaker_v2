/**
 * tokens.css에 정의된 CSS 변수 값을 읽어온다(§7: 컴포넌트에 색상 하드코딩 금지).
 * recharts는 색을 SVG 표현 속성으로 넣기 때문에 `var(--x)` 문자열을 그대로 줄 수 없어, 실제 값으로 변환해 전달한다.
 */
export function cssVar(name: string): string {
  if (typeof window === 'undefined') return 'currentColor'
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || 'currentColor'
}
