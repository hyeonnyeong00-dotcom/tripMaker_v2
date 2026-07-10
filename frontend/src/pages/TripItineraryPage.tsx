import { useMemo, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { DUMMY_TRIP } from '../data/dummyTrip'
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
  useParams()
  const location = useLocation()
  const trip: TripResponse = (location.state as { trip?: TripResponse } | null)?.trip ?? DUMMY_TRIP

  const [activeDay, setActiveDay] = useState(trip.days[0]?.day ?? 1)
  const [dayActivities, setDayActivities] = useState<Record<number, Activity[]>>(() =>
    Object.fromEntries(trip.days.map((d) => [d.day, d.activities])),
  )

  const currentDay = useMemo(() => trip.days.find((d) => d.day === activeDay), [trip.days, activeDay])
  const currentActivities = currentDay ? (dayActivities[currentDay.day] ?? []) : []

  if (!currentDay) return null

  function handleReorder(next: Activity[]) {
    setDayActivities((prev) => ({ ...prev, [currentDay!.day]: next }))
  }

  function handleOptimize() {
    // 목업 단계: 실제 재계산은 API 연결(2단계)에서 구현
    window.alert('동선 최적화는 API 연결 후 동작합니다.')
  }

  function handleReorderRequest() {
    window.alert('재조정 요청은 API 연결 후 동작합니다.')
  }

  function handleSave() {
    window.alert('저장은 API 연결 후 동작합니다.')
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
        <DayTabs days={trip.days} selectedDay={activeDay} onSelect={setActiveDay} />

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
