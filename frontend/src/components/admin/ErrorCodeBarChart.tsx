import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { cssVar } from '../../lib/cssVar'
import type { ErrorSummaryItem } from '../../types/admin'

interface Props {
  items: ErrorSummaryItem[]
  totalErrors: number
}

export function ErrorCodeBarChart({ items, totalErrors }: Props) {
  const primary = cssVar('--color-primary')
  const error = cssVar('--color-error')
  const warning = cssVar('--color-warning-text')
  const textSub = cssVar('--color-text-sub')
  const border = cssVar('--color-border')

  // 대분류별로 막대 색을 구분한다(5xx 계열은 에러 톤, 4xx는 기본 톤).
  const colorFor = (category: string): string => {
    if (category === 'GENERATION_FAILED' || category === 'STORAGE_ERROR') return error
    if (category === 'AUTH_ERROR' || category === 'FORBIDDEN') return warning
    return primary
  }

  return (
    <section className="ad-section">
      <h2 className="ad-section-title">
        에러 코드별 발생 횟수
        <span className="ad-section-sub"> · 총 {totalErrors.toLocaleString()}건</span>
      </h2>
      {items.length === 0 ? (
        <p className="ad-empty-text">기록된 에러가 없습니다.</p>
      ) : (
        <div className="ad-chart-box">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={items} margin={{ top: 8, right: 8, bottom: 4, left: 0 }}>
              <CartesianGrid stroke={border} strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="error_code" tick={{ fill: textSub, fontSize: 12 }} stroke={border} />
              <YAxis allowDecimals={false} tick={{ fill: textSub, fontSize: 12 }} stroke={border} width={36} />
              <Tooltip
                cursor={{ fill: cssVar('--color-bg-sub') }}
                formatter={(value, _name, entry) => [
                  `${Number(value ?? 0).toLocaleString()}건`,
                  String((entry?.payload as ErrorSummaryItem | undefined)?.error_category ?? ''),
                ]}
                contentStyle={{ borderRadius: 12, border: `1px solid ${border}`, fontSize: 12 }}
              />
              <Bar dataKey="count" name="발생 횟수" radius={[6, 6, 0, 0]} maxBarSize={48}>
                {items.map((item) => (
                  <Cell key={item.error_code} fill={colorFor(item.error_category)} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  )
}
