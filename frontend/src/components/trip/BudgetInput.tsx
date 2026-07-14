import { BUDGET_MAX, BUDGET_MIN, BUDGET_STEP } from '../../data/tripFormOptions'

interface Props {
  min: number
  max: number
  onChange: (min: number, max: number) => void
}

/** 1만원 단위 금액은 "16만원"처럼 축약 표기 (§6 라벨 규칙) */
function formatAmount(value: number): string {
  if (value !== 0 && value % 10_000 === 0) {
    return `${(value / 10_000).toLocaleString('ko-KR')}만원`
  }
  return `${value.toLocaleString('ko-KR')}원`
}

export function BudgetInput({ min, max, onChange }: Props) {
  // 두 핸들이 겹쳤을 때 "빠져나올 수 있는 방향"의 핸들이 위로 오도록 z-index를 동적으로 준다.
  // 트랙 끝(또는 상단 절반에서 겹침)이면 min을 위로 → 왼쪽으로 끌어낼 수 있음.
  const minHandleOnTop = min >= BUDGET_MAX || (min === max && min > BUDGET_MAX / 2)
  function handleMinSlider(next: number) {
    onChange(Math.min(next, max), max)
  }

  function handleMaxSlider(next: number) {
    onChange(min, Math.max(next, min))
  }

  function handleMinBox(raw: string) {
    const next = Number(raw)
    if (Number.isNaN(next)) return
    onChange(Math.min(Math.max(next, BUDGET_MIN), max), max)
  }

  function handleMaxBox(raw: string) {
    const next = Number(raw)
    if (Number.isNaN(next)) return
    onChange(min, Math.max(Math.min(next, BUDGET_MAX), min))
  }

  return (
    <div className="tc-budget">
      <div className="tc-budget-values">
        <span>
          {max >= BUDGET_MAX
            ? `${formatAmount(min)} 이상`
            : `${formatAmount(min)} 이상 ${formatAmount(max)} 이하`}
        </span>
      </div>

      <div className="tc-budget-slider">
        <div className="tc-budget-track" />
        <div
          className="tc-budget-track-active"
          style={{
            left: `${(min / BUDGET_MAX) * 100}%`,
            right: `${100 - (max / BUDGET_MAX) * 100}%`,
          }}
        />
        <input
          type="range"
          className="tc-budget-range tc-budget-range--min"
          style={{ zIndex: minHandleOnTop ? 4 : 2 }}
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={BUDGET_STEP}
          value={min}
          onChange={(e) => handleMinSlider(Number(e.target.value))}
        />
        <input
          type="range"
          className="tc-budget-range tc-budget-range--max"
          style={{ zIndex: 3 }}
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={BUDGET_STEP}
          value={max}
          onChange={(e) => handleMaxSlider(Number(e.target.value))}
        />
      </div>

      <div className="tc-budget-boxes">
        <input
          type="number"
          className="tc-input tc-budget-box"
          min={BUDGET_MIN}
          max={max}
          step={BUDGET_STEP}
          value={min}
          onChange={(e) => handleMinBox(e.target.value)}
        />
        <span className="tc-date-sep">–</span>
        <input
          type="number"
          className="tc-input tc-budget-box"
          min={min}
          max={BUDGET_MAX}
          step={BUDGET_STEP}
          value={max}
          onChange={(e) => handleMaxBox(e.target.value)}
        />
      </div>
    </div>
  )
}
