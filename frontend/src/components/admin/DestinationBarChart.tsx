import type { DestinationStat } from '../../types/admin'

interface Props {
  destinations: DestinationStat[]
}

const MAX_ROWS = 8

export function DestinationBarChart({ destinations }: Props) {
  const rows = destinations.slice(0, MAX_ROWS)
  const max = rows.reduce((acc, row) => Math.max(acc, row.count), 0) || 1

  return (
    <section className="ad-section">
      <h2 className="ad-section-title">인기 목적지</h2>
      {rows.length === 0 ? (
        <p className="ad-empty-text">최근 기간 내 생성된 일정이 없습니다.</p>
      ) : (
        <div className="ad-bar-chart" role="img" aria-label="목적지별 생성 건수 막대 차트">
          {rows.map((row) => (
            <div className="ad-bar-row" key={row.destination} title={`${row.destination}: ${row.count}건`}>
              <span className="ad-bar-label">{row.destination}</span>
              <div className="ad-bar-track">
                <div className="ad-bar-fill" style={{ width: `${(row.count / max) * 100}%` }} />
              </div>
              <span className="ad-bar-value">{row.count.toLocaleString()}</span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
