interface Props {
  reason: string | null
  onOptimize: () => void
}

/** CLAUDE.md 6절 고정 문구/톤 — 경고가 아닌 참고/제안 톤 배지 */
export function RouteWarningBanner({ reason, onOptimize }: Props) {
  return (
    <div className="it-warning-banner">
      <span className="it-warning-icon">💡</span>
      <div className="it-warning-body">
        <p className="it-warning-text">이 순서면 이동 시간이 길어질 수 있어요</p>
        {reason && <p className="it-warning-reason">{reason}</p>}
      </div>
      <button type="button" className="it-warning-cta" onClick={onOptimize}>
        동선 최적화
      </button>
    </div>
  )
}
