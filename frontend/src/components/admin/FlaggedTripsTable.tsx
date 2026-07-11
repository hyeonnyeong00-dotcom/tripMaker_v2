import { useNavigate } from 'react-router-dom'
import type { FlaggedTrip } from '../../types/admin'

interface Props {
  trips: FlaggedTrip[]
}

function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function FlaggedTripsTable({ trips }: Props) {
  const navigate = useNavigate()

  if (trips.length === 0) {
    return (
      <section className="ad-section">
        <h2 className="ad-section-title">품질 모니터링</h2>
        <p className="ad-empty-text">flagged된 일정이 없습니다.</p>
      </section>
    )
  }

  return (
    <section className="ad-section">
      <h2 className="ad-section-title">품질 모니터링</h2>
      <div className="ad-table-wrap">
        <table className="ad-table">
          <thead>
            <tr>
              <th>목적지</th>
              <th>Day</th>
              <th>사유</th>
              <th>일시</th>
            </tr>
          </thead>
          <tbody>
            {trips.map((trip) => (
              <tr
                key={`${trip.trip_id}-${trip.day}`}
                className="ad-table-row"
                onClick={() => navigate(`/admin/trips/${trip.trip_id}?day=${trip.day}`)}
              >
                <td>{trip.destination}</td>
                <td>Day {trip.day}</td>
                <td className="ad-table-reason">{trip.reason ?? '-'}</td>
                <td>{formatDateTime(trip.flagged_at)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
