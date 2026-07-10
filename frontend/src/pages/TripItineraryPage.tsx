import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { getTrip } from '../api/trips'
import { extractErrorMessage } from '../lib/apiError'
import { TripMap } from '../components/trip/TripMap'
import { DayTabs } from '../components/trip/DayTabs'
import { RouteWarningBanner } from '../components/trip/RouteWarningBanner'
import { ActivityTimeline } from '../components/trip/ActivityTimeline'
import type { Activity, TripResponse } from '../types/trip'
import '../components/trip/itinerary.css'

function durationBadgeLabel(durationDays: number): string {
  const nights = Math.max(0, durationDays - 1)
  return `${nights}박 ${durationDays}일`
}

export default function TripItineraryPage() {
  const { tripId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const initialTrip = (location.state as { trip?: TripResponse } | null)?.trip

  const tripQuery = useQuery({
    queryKey: ['trip', tripId],
    queryFn: () => getTrip(tripId!),
    enabled: !!tripId,
    initialData: initialTrip && initialTrip.trip_id === tripId ? initialTrip : undefined,
    retry: (failureCount, error) => {
      if (isAxiosError(error) && error.response && error.response.status < 500) return false
      return failureCount < 3
    },
  })

  const trip = tripQuery.data

  const [activeDay, setActiveDay] = useState<number | null>(null)
  const [dayActivities, setDayActivities] = useState<Record<number, Activity[]> | null>(null)

  useEffect(() => {
    if (trip && dayActivities === null) {
      setActiveDay(trip.days[0]?.day ?? 1)
      setDayActivities(Object.fromEntries(trip.days.map((d) => [d.day, d.activities])))
    }
  }, [trip, dayActivities])

  const currentDay = useMemo(
    () => trip?.days.find((d) => d.day === activeDay),
    [trip, activeDay],
  )
  const currentActivities = currentDay ? (dayActivities?.[currentDay.day] ?? []) : []

  if (tripQuery.isLoading) {
    return (
      <div className="it-shell">
        <p className="it-status-text">불러오는 중...</p>
      </div>
    )
  }

  if (tripQuery.isError || !trip) {
    return (
      <div className="it-shell">
        <p className="it-status-text">
          {extractErrorMessage(tripQuery.error, '여행 정보를 불러오지 못했습니다.')}
        </p>
        <button type="button" className="it-footer-btn it-footer-btn--primary" onClick={() => navigate('/')}>
          홈으로
        </button>
      </div>
    )
  }

  if (!currentDay) return null

  function handleReorder(next: Activity[]) {
    setDayActivities((prev) => ({ ...prev, [currentDay!.day]: next }))
  }

  function handleOptimize() {
    // 재조정 API 연결은 M6에서 구현
    window.alert('동선 최적화는 다음 마일스톤에서 동작합니다.')
  }

  function handleReorderRequest() {
    window.alert('재조정 요청은 다음 마일스톤에서 동작합니다.')
  }

  function handleSave() {
    window.alert('저장은 이미 완료되어 있어요. 저장한 여행 목록은 다음 마일스톤에서 구현됩니다.')
  }

  return (
    <div className="it-shell">
      <header className="it-header">
        <div>
          <h1 className="it-title">{trip.destination} 일정</h1>
          <span className="it-duration-badge">{durationBadgeLabel(trip.duration_days)}</span>
        </div>
        <button type="button" className="it-save-btn" onClick={handleSave}>
          저장
        </button>
      </header>

      <TripMap activities={currentActivities} flagged={currentDay.route_warning.flagged} />

      <div className="it-body">
        <DayTabs days={trip.days} selectedDay={activeDay!} onSelect={setActiveDay} />

        <div className="it-theme-row">
          <h2 className="it-theme-text">{currentDay.theme ?? `Day ${currentDay.day}`}</h2>
          {currentDay.last_modified && <span className="it-modified-badge">변경됨</span>}
        </div>

        {currentDay.route_warning.flagged && (
          <RouteWarningBanner reason={currentDay.route_warning.reason} onOptimize={handleOptimize} />
        )}

        <ActivityTimeline activities={currentActivities} onReorder={handleReorder} />
      </div>

      <footer className="it-footer">
        <button type="button" className="it-footer-btn it-footer-btn--outline" onClick={handleOptimize}>
          동선 최적화
        </button>
        <button type="button" className="it-footer-btn it-footer-btn--primary" onClick={handleReorderRequest}>
          재조정
        </button>
      </footer>
    </div>
  )
}
