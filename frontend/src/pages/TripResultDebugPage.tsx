import { useLocation, useNavigate, useParams } from 'react-router-dom'
import type { TripResponse } from '../types/trip'

/** 임시 페이지 — 일정표 UI는 M5에서 구현. 지금은 생성 결과 JSON을 그대로 덤프해 확인한다. */
export default function TripResultDebugPage() {
  const { tripId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const trip = (location.state as { trip?: TripResponse } | null)?.trip

  if (!trip) {
    return (
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 24px' }}>
        <p style={{ color: 'var(--color-text-sub)' }}>
          새로고침 등으로 생성 결과가 유실됐습니다. trip_id: {tripId}
        </p>
        <button type="button" className="auth-submit" onClick={() => navigate('/trips/new')}>
          다시 생성하기
        </button>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 720, margin: '0 auto', padding: '32px 24px' }}>
      <h1 style={{ fontSize: 20, marginBottom: 8 }}>생성 결과 (임시 JSON 덤프)</h1>
      <p style={{ color: 'var(--color-text-sub)', marginBottom: 16 }}>
        일정표 화면은 M5에서 구현됩니다. trip_id: {trip.trip_id}
      </p>
      <pre
        style={{
          background: 'var(--color-bg-sub)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-container)',
          padding: 16,
          overflowX: 'auto',
          fontSize: 13,
        }}
      >
        {JSON.stringify(trip, null, 2)}
      </pre>
    </div>
  )
}
