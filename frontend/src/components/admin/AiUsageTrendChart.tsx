import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { cssVar } from '../../lib/cssVar'
import type { AiUsageDailyPoint } from '../../types/admin'

interface Props {
  points: AiUsageDailyPoint[]
  days: number
}

/** "2026-09-16" → "09/16" (축 라벨이 길어지지 않도록) */
function shortDate(date: string): string {
  const [, month, day] = date.split('-')
  return month && day ? `${month}/${day}` : date
}

export function AiUsageTrendChart({ points, days }: Props) {
  const primary = cssVar('--color-primary')
  const textSub = cssVar('--color-text-sub')
  const border = cssVar('--color-border')
  const modified = cssVar('--color-modified-text')
  const warning = cssVar('--color-warning-text')

  const hasData = points.some((p) => p.calls > 0)

  return (
    <section className="ad-section">
      <h2 className="ad-section-title">일별 호출 수 · 토큰 추이 (최근 {days}일)</h2>
      {!hasData ? (
        <p className="ad-empty-text">최근 {days}일 내 AI 호출 기록이 없습니다.</p>
      ) : (
        <div className="ad-chart-box">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={points} margin={{ top: 8, right: 8, bottom: 4, left: 0 }}>
              <CartesianGrid stroke={border} strokeDasharray="3 3" vertical={false} />
              <XAxis
                dataKey="date"
                tickFormatter={shortDate}
                tick={{ fill: textSub, fontSize: 12 }}
                stroke={border}
              />
              <YAxis
                yAxisId="calls"
                allowDecimals={false}
                tick={{ fill: textSub, fontSize: 12 }}
                stroke={border}
                width={36}
              />
              <YAxis
                yAxisId="tokens"
                orientation="right"
                tick={{ fill: textSub, fontSize: 12 }}
                stroke={border}
                width={52}
              />
              <Tooltip
                labelFormatter={(value) => String(value)}
                formatter={(value, name) => [Number(value ?? 0).toLocaleString(), String(name ?? '')]}
                contentStyle={{ borderRadius: 12, border: `1px solid ${border}`, fontSize: 12 }}
              />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              <Line
                yAxisId="calls"
                type="monotone"
                dataKey="calls"
                name="호출 수"
                stroke={primary}
                strokeWidth={2}
                dot={{ r: 3 }}
              />
              <Line
                yAxisId="tokens"
                type="monotone"
                dataKey="input_tokens"
                name="입력 토큰"
                stroke={modified}
                strokeWidth={2}
                dot={false}
              />
              <Line
                yAxisId="tokens"
                type="monotone"
                dataKey="output_tokens"
                name="출력 토큰"
                stroke={warning}
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  )
}
