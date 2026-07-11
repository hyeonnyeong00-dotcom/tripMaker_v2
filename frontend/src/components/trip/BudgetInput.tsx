import { BUDGET_MAX, BUDGET_MIN, BUDGET_STEP } from '../../data/tripFormOptions'

interface Props {
  min: number
  max: number | null
  onChange: (min: number, max: number | null) => void
}

function formatWon(value: number): string {
  return `${value.toLocaleString('ko-KR')}원`
}

export function BudgetInput({ min, max, onChange }: Props) {
  const maxValue = max ?? BUDGET_MAX

  function handleMinSlider(next: number) {
    onChange(Math.min(next, maxValue), max)
  }

  function handleMaxSlider(next: number) {
    const clamped = Math.max(next, min)
    onChange(min, clamped >= BUDGET_MAX ? null : clamped)
  }

  function handleMinBox(raw: string) {
    const next = Number(raw)
    if (Number.isNaN(next)) return
    onChange(Math.min(Math.max(next, BUDGET_MIN), maxValue), max)
  }

  function handleMaxBox(raw: string) {
    const next = Number(raw)
    if (Number.isNaN(next)) return
    const clamped = Math.max(Math.min(next, BUDGET_MAX), min)
    onChange(min, clamped >= BUDGET_MAX ? null : clamped)
  }

  return (
    <div className="tc-budget">
      <div className="tc-budget-values">
        <span>{formatWon(min)}</span>
        <span>–</span>
        <span>{max === null ? '50만원 이상' : formatWon(max)}</span>
      </div>

      <div className="tc-budget-slider">
        <div className="tc-budget-track" />
        <div
          className="tc-budget-track-active"
          style={{
            left: `${(min / BUDGET_MAX) * 100}%`,
            right: `${100 - (maxValue / BUDGET_MAX) * 100}%`,
          }}
        />
        <input
          type="range"
          className="tc-budget-range tc-budget-range--min"
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={BUDGET_STEP}
          value={min}
          onChange={(e) => handleMinSlider(Number(e.target.value))}
        />
        <input
          type="range"
          className="tc-budget-range tc-budget-range--max"
          min={BUDGET_MIN}
          max={BUDGET_MAX}
          step={BUDGET_STEP}
          value={maxValue}
          onChange={(e) => handleMaxSlider(Number(e.target.value))}
        />
      </div>

      <div className="tc-budget-boxes">
        <input
          type="number"
          className="tc-input tc-budget-box"
          min={BUDGET_MIN}
          max={maxValue}
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
          value={maxValue}
          onChange={(e) => handleMaxBox(e.target.value)}
        />
      </div>
    </div>
  )
}
