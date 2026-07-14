import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getTrip } from '../../api/trips'
import { extractErrorMessage } from '../../lib/apiError'
import { AdminLayout } from '../../components/admin/AdminLayout'
import { TripMap } from '../../components/trip/TripMap'
import { DayTabs } from '../../components/trip/DayTabs'
import { RouteWarningBanner } from '../../components/trip/RouteWarningBanner'
import { ActivityTimeline } from '../../components/trip/ActivityTimeline'
import '../../components/trip/itinerary.css'
import '../../components/admin/admin.css'

function durationBadgeLabel(durationDays: number): string {
  const nights = Math.max(0, durationDays - 1)
  return `${nights}박 ${durationDays}일`
}

export default function AdminTripDetailPage() {
  const { tripId } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const requestedDay = Number(searchParams.get('day'))

  const tripQuery = useQuery({
    queryKey: ['trip', tripId],
    queryFn: () => getTrip(tripId!),
    enabled: !!tripId,
  })

  const trip = tripQuery.data
  const [activeDay, setActiveDay] = useState<number | null>(null)

  const currentDay = useMemo(() => {
    if (!trip) return undefined
    const initial = activeDay ?? (Number.isFinite(requestedDay) && requestedDay > 0 ? requestedDay : trip.days[0]?.day)
    return trip.days.find((d) => d.day === initial) ?? trip.days[0]
  }, [trip, activeDay, requestedDay])

  if (tripQuery.isLoading) {
    return (
      <AdminLayout>
        <div className="it-shell">
          <p className="it-status-text">불러오는 중...</p>
        </div>
      </AdminLayout>
    )
  }

  if (tripQuery.isError || !trip || !currentDay) {
    return (
      <AdminLayout>
        <div className="it-shell">
          <p className="it-status-text">{extractErrorMessage(tripQuery.error, '여행 정보를 불러오지 못했습니다.')}</p>
          <button type="button" className="it-footer-btn it-footer-btn--primary" onClick={() => navigate('/admin')}>
            대시보드로
          </button>
        </div>
      </AdminLayout>
    )
  }

  return (
    <AdminLayout>
      <div className="it-shell">
        <header className="it-header">
          <div>
            <span className="ad-readonly-badge">읽기 전용</span>
            <h1 className="it-title">{trip.destination} 일정</h1>
            <span className="it-duration-badge">{durationBadgeLabel(trip.duration_days)}</span>
          </div>
          <button type="button" className="it-save-btn" onClick={() => navigate('/admin')}>
            목록으로
          </button>
        </header>

        <TripMap activities={currentDay.activities} flagged={currentDay.route_warning.flagged} />

        <div className="it-body">
          <DayTabs days={trip.days} selectedDay={currentDay.day} onSelect={setActiveDay} />

          <div className="it-theme-row">
            <h2 className="it-theme-text">{currentDay.theme ?? `Day ${currentDay.day}`}</h2>
            {currentDay.last_modified && <span className="it-modified-badge">변경됨</span>}
          </div>

          {currentDay.route_warning.flagged && <RouteWarningBanner reason={currentDay.route_warning.reason} />}

          <ActivityTimeline activities={currentDay.activities} onReorder={() => {}} readOnly />
        </div>
      </div>
    </AdminLayout>
  )
}
