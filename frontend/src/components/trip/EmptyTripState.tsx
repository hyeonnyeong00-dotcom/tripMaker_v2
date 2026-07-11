import { useNavigate } from 'react-router-dom'

export function EmptyTripState() {
  const navigate = useNavigate()

  return (
    <div className="tl-empty">
      <span className="tl-empty-icon">🧳</span>
      <p className="tl-empty-title">
        일정이 존재하지 않네요.
        <br />
        일정을 만들까요?
      </p>
      <p className="tl-empty-subline">목적지만 정해오세요. 동선은 AI가 짤게요.</p>
      <button type="button" className="tl-empty-cta" onClick={() => navigate('/trips/new')}>
        AI 추천 일정 만들기
      </button>
    </div>
  )
}
