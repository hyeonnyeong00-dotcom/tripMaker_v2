import { useState } from 'react'

interface Props {
  label: string
  value: string
  onConfirm: (value: string) => void
  onClose: () => void
}

const PERIODS: Array<'오전' | '오후'> = ['오전', '오후']
const HOURS = Array.from({ length: 12 }, (_, i) => i + 1)
const MINUTES = [0, 30]

function parseValue(value: string): { period: '오전' | '오후'; hour12: number; minute: number } {
  const [hourStr, minuteStr] = value.split(':')
  const hour24 = Number(hourStr)
  const minute = Number(minuteStr) >= 30 ? 30 : 0
  const period: '오전' | '오후' = hour24 < 12 ? '오전' : '오후'
  const hour12raw = hour24 % 12
  const hour12 = hour12raw === 0 ? 12 : hour12raw
  return { period, hour12, minute }
}

function toValue(period: '오전' | '오후', hour12: number, minute: number): string {
  let hour24 = hour12 % 12
  if (period === '오후') hour24 += 12
  return `${String(hour24).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

export function TimeWheelPicker({ label, value, onConfirm, onClose }: Props) {
  const initial = parseValue(value)
  const [period, setPeriod] = useState(initial.period)
  const [hour12, setHour12] = useState(initial.hour12)
  const [minute, setMinute] = useState(initial.minute)

  function handleConfirm() {
    onConfirm(toValue(period, hour12, minute))
    onClose()
  }

  return (
    <div className="tc-wheel-overlay" onClick={onClose}>
      <div className="tc-wheel-card" onClick={(e) => e.stopPropagation()}>
        <p className="tc-wheel-title">{label}</p>
        <div className="tc-wheel-columns">
          <div className="tc-wheel-column">
            {PERIODS.map((p) => (
              <button
                key={p}
                type="button"
                className={`tc-wheel-item ${period === p ? 'tc-wheel-item--active' : ''}`}
                onClick={() => setPeriod(p)}
              >
                {p}
              </button>
            ))}
          </div>
          <div className="tc-wheel-column">
            {HOURS.map((h) => (
              <button
                key={h}
                type="button"
                className={`tc-wheel-item ${hour12 === h ? 'tc-wheel-item--active' : ''}`}
                onClick={() => setHour12(h)}
              >
                {h}시
              </button>
            ))}
          </div>
          <div className="tc-wheel-column">
            {MINUTES.map((m) => (
              <button
                key={m}
                type="button"
                className={`tc-wheel-item ${minute === m ? 'tc-wheel-item--active' : ''}`}
                onClick={() => setMinute(m)}
              >
                {String(m).padStart(2, '0')}분
              </button>
            ))}
          </div>
        </div>
        <button type="button" className="tc-cta tc-wheel-confirm" onClick={handleConfirm}>
          확인
        </button>
      </div>
    </div>
  )
}
