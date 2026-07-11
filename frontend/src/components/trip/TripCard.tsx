import { useNavigate } from 'react-router-dom'
import type { TripSummary } from '../../types/trip'
import { getDestinationTheme } from '../../data/thumbnailPalette'

interface Props {
  trip: TripSummary
}

function toISODateString(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function daysBetweenISO(a: string, b: string): number {
  return Math.round((new Date(b).getTime() - new Date(a).getTime()) / 86_400_000)
}

function getDDayLabel(startDate: string, endDate: string): string | null {
  const today = toISODateString(new Date())
  const diff = daysBetweenISO(today, startDate)
  if (diff > 0) return `D-${diff}`
  if (diff === 0) return 'D-DAY'
  if (today <= endDate) return '여행중'
  return null
}

function formatDateLabel(iso: string): string {
  const [, month, day] = iso.split('-')
  return `${Number(month)}.${Number(day)}`
}

function formatDateRange(startDate: string, endDate: string, durationDays: number): string {
  const nights = Math.max(0, durationDays - 1)
  return `${formatDateLabel(startDate)} – ${formatDateLabel(endDate)} · ${nights}박 ${durationDays}일`
}

export function TripCard({ trip }: Props) {
  const navigate = useNavigate()
  const theme = getDestinationTheme(trip.destination)
  const dDayLabel = getDDayLabel(trip.start_date, trip.end_date)

  return (
    <button type="button" className="tl-card" onClick={() => navigate(`/trips/${trip.trip_id}`)}>
      <div className="tl-thumbnail" style={{ background: theme.bg, color: theme.fg }}>
        <span className="tl-thumbnail-icon">📍</span>
      </div>
      <div className="tl-card-body">
        <div className="tl-card-title-row">
          <h3 className="tl-card-title">{trip.destination}</h3>
          {dDayLabel && <span className="tl-dday-badge">{dDayLabel}</span>}
        </div>
        <p className="tl-card-dates">{formatDateRange(trip.start_date, trip.end_date, trip.duration_days)}</p>
        {trip.preferences.length > 0 && (
          <div className="tl-card-chips">
            {trip.preferences.map((pref) => (
              <span key={pref} className="tc-preference-chip">
                {pref}
              </span>
            ))}
          </div>
        )}
      </div>
    </button>
  )
}
