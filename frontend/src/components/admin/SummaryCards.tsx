interface Props {
  totalTrips: number
  periodDays: number
  flaggedCount: number
  flaggedRatio: number
  activeTemplateCount: number
}

export function SummaryCards({ totalTrips, periodDays, flaggedCount, flaggedRatio, activeTemplateCount }: Props) {
  return (
    <div className="ad-summary-grid">
      <div className="ad-summary-card">
        <span className="ad-summary-label">생성 수 (최근 {periodDays}일)</span>
        <span className="ad-summary-value">{totalTrips.toLocaleString()}</span>
      </div>
      <div className="ad-summary-card">
        <span className="ad-summary-label">flagged 수 · 비율</span>
        <span className="ad-summary-value">
          {flaggedCount.toLocaleString()}
          <span className="ad-summary-value-sub"> · {(flaggedRatio * 100).toFixed(1)}%</span>
        </span>
      </div>
      <div className="ad-summary-card">
        <span className="ad-summary-label">활성 템플릿</span>
        <span className="ad-summary-value">{activeTemplateCount.toLocaleString()}</span>
      </div>
    </div>
  )
}
