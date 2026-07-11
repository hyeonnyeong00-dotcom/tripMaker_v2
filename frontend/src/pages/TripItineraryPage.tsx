import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { getTrip, reorderTrip } from '../api/trips'
import { extractErrorMessage } from '../lib/apiError'
import { AppHeader } from '../components/layout/AppHeader'
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
  const queryClient = useQueryClient()
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
  const [reorderError, setReorderError] = useState<string | null>(null)

  useEffect(() => {
    if (trip && dayActivities === null) {
      setActiveDay(trip.days[0]?.day ?? 1)
      setDayActivities(Object.fromEntries(trip.days.map((d) => [d.day, d.activities])))
    }
  }, [trip, dayActivities])

  const reorderMutation = useMutation({
    mutationFn: (vars: { day: number; newActivityOrder: string[] }) =>
      reorderTrip(tripId!, { day: vars.day, new_activity_order: vars.newActivityOrder }),
    onSuccess: (response) => {
      queryClient.setQueryData(['trip', tripId], response)
      setDayActivities(Object.fromEntries(response.days.map((d) => [d.day, d.activities])))
      setReorderError(null)
    },
    onError: (error) => {
      setReorderError(extractErrorMessage(error, '재조정에 실패했습니다. 잠시 후 다시 시도해주세요.'))
    },
  })

  const currentDay = useMemo(
    () => trip?.days.find((d) => d.day === activeDay),
    [trip, activeDay],
  )
  const currentActivities = currentDay ? (dayActivities?.[currentDay.day] ?? []) : []

  if (tripQuery.isLoading) {
    return (
      <>
        <AppHeader />
        <div className="it-shell">
          <p className="it-status-text">불러오는 중...</p>
        </div>
      </>
    )
  }

  if (tripQuery.isError || !trip) {
    return (
      <>
        <AppHeader />
        <div className="it-shell">
          <p className="it-status-text">
            {extractErrorMessage(tripQuery.error, '여행 정보를 불러오지 못했습니다.')}
          </p>
          <button type="button" className="it-footer-btn it-footer-btn--primary" onClick={() => navigate('/')}>
            홈으로
          </button>
        </div>
      </>
    )
  }

  if (!currentDay) return null

  function handleReorder(next: Activity[]) {
    setDayActivities((prev) => ({ ...prev, [currentDay!.day]: next }))
  }

  function handleReorderRequest() {
    if (!currentDay) return
    setReorderError(null)
    reorderMutation.mutate({ day: currentDay.day, newActivityOrder: currentActivities.map((a) => a.id) })
  }

  function handleSave() {
    window.alert('저장은 이미 완료되어 있어요. 저장한 여행 목록은 다음 마일스톤에서 구현됩니다.')
  }

  return (
    <>
      <AppHeader />
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
          <RouteWarningBanner reason={currentDay.route_warning.reason} onOptimize={handleReorderRequest} />
        )}

        {reorderError && <p className="it-error-text">{reorderError}</p>}

        <div style={reorderMutation.isPending ? { opacity: 0.5, pointerEvents: 'none' } : undefined}>
          <ActivityTimeline activities={currentActivities} onReorder={handleReorder} />
        </div>
      </div>

      <footer className="it-footer">
        <button
          type="button"
          className="it-footer-btn it-footer-btn--outline"
          disabled={reorderMutation.isPending}
          onClick={handleReorderRequest}
        >
          동선 최적화
        </button>
        <button
          type="button"
          className="it-footer-btn it-footer-btn--primary"
          disabled={reorderMutation.isPending}
          onClick={handleReorderRequest}
        >
          {reorderMutation.isPending ? '재조정 중...' : '재조정'}
        </button>
      </footer>
      </div>
    </>
  )
}
