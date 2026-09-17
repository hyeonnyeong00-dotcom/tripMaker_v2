import type { AiUsageSummary } from '../../types/admin'

interface Props {
  summary: AiUsageSummary
}

/** 예산 소진율이 이 값을 넘으면 게이지를 경고 톤으로 바꾼다. */
const WARN_RATIO = 0.8

function formatUsd(value: number): string {
  return `$${value.toFixed(4)}`
}

export function AiUsageCards({ summary }: Props) {
  const ratio = Math.min(summary.budget_used_ratio, 1)
  const isWarn = summary.budget_used_ratio >= WARN_RATIO

  return (
    <>
      <div className="ad-summary-grid">
        <div className="ad-summary-card">
          <span className="ad-summary-label">총 호출 수</span>
          <span className="ad-summary-value">
            {summary.total_calls.toLocaleString()}
            <span className="ad-summary-value-sub"> · 성공 {summary.success_calls.toLocaleString()}</span>
          </span>
        </div>

        <div className="ad-summary-card">
          <span className="ad-summary-label">캐시 히트율</span>
          <span className="ad-summary-value">
            {(summary.cache_hit_rate * 100).toFixed(1)}%
            <span className="ad-summary-value-sub"> · {summary.cache_hits.toLocaleString()}건</span>
          </span>
        </div>

        <div className="ad-summary-card">
          <span className="ad-summary-label">누적 예상 비용</span>
          <span className="ad-summary-value">{formatUsd(summary.total_cost_usd)}</span>
          <div
            className="ad-gauge-track"
            role="img"
            aria-label={`예산 ${formatUsd(summary.budget_usd)} 대비 ${(summary.budget_used_ratio * 100).toFixed(2)}% 사용`}
          >
            <div
              className={`ad-gauge-fill ${isWarn ? 'ad-gauge-fill--warn' : ''}`}
              style={{ width: `${Math.max(ratio * 100, 1)}%` }}
            />
          </div>
          <span className="ad-gauge-caption">
            예산 {formatUsd(summary.budget_usd)} 대비 {(summary.budget_used_ratio * 100).toFixed(2)}%
          </span>
        </div>
      </div>

      <div className="ad-metric-row">
        <span className="ad-metric-label">평균 응답 시간</span>
        <span className="ad-metric-value">{summary.avg_duration_ms.toLocaleString()}ms</span>
        <span className="ad-metric-note">캐시 히트를 제외한 실제 AI 호출 기준</span>
      </div>
    </>
  )
}
