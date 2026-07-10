import { BUDGET_QUICK_OPTIONS } from '../../data/tripFormOptions'

interface Props {
  value: string
  onChange: (value: string) => void
}

export function BudgetInput({ value, onChange }: Props) {
  return (
    <div>
      <div className="tc-quick-chips">
        {BUDGET_QUICK_OPTIONS.map((option) => (
          <button
            key={option}
            type="button"
            className={`tc-quick-chip ${value === option ? 'tc-quick-chip--active' : ''}`}
            onClick={() => onChange(option)}
          >
            {option}
          </button>
        ))}
      </div>
      <input
        className="tc-input"
        value={value}
        maxLength={30}
        placeholder="예산을 자유롭게 입력하세요"
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  )
}
