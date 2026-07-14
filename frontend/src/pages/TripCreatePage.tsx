import { useState } from 'react'
import type { MouseEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { AppHeader } from '../components/layout/AppHeader'
import { NEARBY_MAP } from '../data/destinations'
import { DestinationPickerModal } from '../components/trip/DestinationPickerModal'
import { BudgetInput } from '../components/trip/BudgetInput'
import { CompanionChips } from '../components/trip/CompanionChips'
import { PreferenceChips } from '../components/trip/PreferenceChips'
import { ActiveTimeInput } from '../components/trip/ActiveTimeInput'
import { GenerationLoadingOverlay } from '../components/trip/GenerationLoadingOverlay'
import { createTrip } from '../api/trips'
import { extractErrorMessage } from '../lib/apiError'
import { BUDGET_MAX, DEFAULT_ACTIVE_END_TIME, DEFAULT_ACTIVE_START_TIME } from '../data/tripFormOptions'
import '../components/trip/tripForm.css'
import '../components/trip/destinationPicker.css'

function getNearbyLabel(destination: string): string {
  const nearby = NEARBY_MAP[destination.trim()]
  if (nearby && nearby.length > 0) {
    return `${nearby.join('·')}도 함께 볼까요?`
  }
  return '근교 지역도 함께 볼까요?'
}

function getDurationBadge(startDate: string, endDate: string): string | null {
  if (!startDate || !endDate) return null
  const nights = Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86_400_000)
  if (nights <= 0) return null
  return `${nights}박 ${nights + 1}일`
}

function getTodayDateString(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const MAX_TRIP_DAYS = 30
const DURATION_LIMIT_MESSAGE = '여행 기간은 최대 30일까지 선택 가능합니다'

/** 시작일 포함 일수 (start === end → 1일) */
function inclusiveDays(startDate: string, endDate: string): number {
  return Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86_400_000) + 1
}

function addDaysISO(dateStr: string, days: number): string {
  const date = new Date(`${dateStr}T00:00:00`)
  date.setDate(date.getDate() + days)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function openDatePicker(event: MouseEvent<HTMLInputElement>) {
  const input = event.currentTarget as HTMLInputElement & { showPicker?: () => void }
  if (typeof input.showPicker === 'function') {
    input.showPicker()
  }
}

export default function TripCreatePage() {
  const navigate = useNavigate()
  const [destination, setDestination] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [includeNearby, setIncludeNearby] = useState(false)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [durationError, setDurationError] = useState<string | null>(null)
  const [companion, setCompanion] = useState('')
  const [budgetMin, setBudgetMin] = useState(0)
  const [budgetMax, setBudgetMax] = useState(BUDGET_MAX)
  const [preferences, setPreferences] = useState<string[]>([])
  const [activeStartTime, setActiveStartTime] = useState(DEFAULT_ACTIVE_START_TIME)
  const [activeEndTime, setActiveEndTime] = useState(DEFAULT_ACTIVE_END_TIME)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const createTripMutation = useMutation({
    mutationFn: createTrip,
    onSuccess: (trip) => {
      navigate(`/trips/${trip.trip_id}`, { state: { trip } })
    },
    onError: (error) => {
      setErrorMessage(extractErrorMessage(error, '일정 생성에 실패했습니다. 잠시 후 다시 시도해주세요.'))
    },
  })

  const trimmedDestination = destination.trim()
  const durationBadge = getDurationBadge(startDate, endDate)
  const canSubmit =
    trimmedDestination !== '' && durationBadge !== null && companion !== '' && preferences.length > 0

  function handleSelectDestination(name: string) {
    setDestination(name)
    setModalOpen(false)
  }

  function handleStartDateChange(value: string) {
    setStartDate(value)
    // 시작일 변경으로 기존 종료일이 30일을 넘게 되면 종료일을 되돌리고 안내
    if (value && endDate && inclusiveDays(value, endDate) > MAX_TRIP_DAYS) {
      setEndDate('')
      setDurationError(DURATION_LIMIT_MESSAGE)
    } else {
      setDurationError(null)
    }
  }

  function handleEndDateChange(value: string) {
    // 달력 max로 막지만, 직접 입력 등 우회 경로는 여기서 되돌린다
    if (startDate && value && inclusiveDays(startDate, value) > MAX_TRIP_DAYS) {
      setDurationError(DURATION_LIMIT_MESSAGE)
      return
    }
    setDurationError(null)
    setEndDate(value)
  }

  function handleSubmit() {
    setErrorMessage(null)
    createTripMutation.mutate({
      destination: trimmedDestination,
      start_date: startDate,
      end_date: endDate,
      budget_min: budgetMin,
      // 최대 핸들이 트랙 끝(BUDGET_MAX)이면 상한 없음 → null 전송 (§5.6 2-2)
      budget_max: budgetMax >= BUDGET_MAX ? null : budgetMax,
      companion,
      preferences,
      include_nearby: includeNearby,
      active_start_time: activeStartTime,
      active_end_time: activeEndTime,
    })
  }

  return (
    <>
      <AppHeader />
      <div className="tc-shell">
      <div className="tc-header">
        <h1 className="tc-headline">어디로 떠나볼까요?</h1>
        <p className="tc-subline">목적지만 정해오세요. 동선은 AI가 짤게요.</p>
      </div>

      <div className="tc-form">
        <div className="tc-field">
          <label className="tc-label" htmlFor="destination">
            목적지
          </label>
          <div className="tc-destination-field">
            <span className="tc-destination-pin">📍</span>
            <input
              id="destination"
              className="tc-input tc-destination-input"
              value={destination}
              placeholder="지역을 선택해주세요"
              onChange={(e) => setDestination(e.target.value)}
              onClick={() => setModalOpen(true)}
            />
          </div>
        </div>

        {trimmedDestination !== '' && (
          <div className="tc-nearby-row">
            <p className="tc-nearby-label">{getNearbyLabel(destination)}</p>
            <button
              type="button"
              role="switch"
              aria-checked={includeNearby}
              className={`tc-switch ${includeNearby ? 'tc-switch--on' : ''}`}
              onClick={() => setIncludeNearby((v) => !v)}
            >
              <span className="tc-switch-thumb" />
            </button>
          </div>
        )}

        <div className="tc-field">
          <label className="tc-label">여행 기간</label>
          <div className="tc-date-row">
            <input
              type="date"
              className="tc-input"
              value={startDate}
              min={getTodayDateString()}
              onClick={openDatePicker}
              onChange={(e) => handleStartDateChange(e.target.value)}
            />
            <span className="tc-date-sep">–</span>
            <input
              type="date"
              className="tc-input"
              value={endDate}
              min={startDate || getTodayDateString()}
              max={startDate ? addDaysISO(startDate, MAX_TRIP_DAYS - 1) : undefined}
              onClick={openDatePicker}
              onChange={(e) => handleEndDateChange(e.target.value)}
            />
          </div>
          {durationError && <p className="tc-error">{durationError}</p>}
          {durationBadge && <span className="tc-duration-badge">{durationBadge}</span>}
        </div>

        <div className="tc-field">
          <label className="tc-label">누구와</label>
          <CompanionChips value={companion} onChange={setCompanion} />
        </div>

        <div className="tc-field">
          <label className="tc-label">예산</label>
          <BudgetInput min={budgetMin} max={budgetMax} onChange={(min, max) => { setBudgetMin(min); setBudgetMax(max) }} />
        </div>

        <div className="tc-field">
          <label className="tc-label">취향</label>
          <PreferenceChips selected={preferences} onChange={setPreferences} />
        </div>

        <div className="tc-field">
          <label className="tc-label">활동 시간대</label>
          <ActiveTimeInput
            startTime={activeStartTime}
            endTime={activeEndTime}
            onChange={(start, end) => { setActiveStartTime(start); setActiveEndTime(end) }}
          />
        </div>

        {errorMessage && <p className="tc-error">{errorMessage}</p>}

        <button
          type="button"
          className="tc-cta"
          disabled={!canSubmit || createTripMutation.isPending}
          onClick={handleSubmit}
        >
          {trimmedDestination ? `${trimmedDestination} 일정 만들기` : '일정 만들기'}
        </button>
      </div>

      <DestinationPickerModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSelect={handleSelectDestination}
      />

      <GenerationLoadingOverlay
        visible={createTripMutation.isPending}
        destinationLabel={destination}
        onComplete={() => {}}
      />
      </div>
    </>
  )
}
