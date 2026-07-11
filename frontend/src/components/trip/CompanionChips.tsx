import { COMPANION_OPTIONS } from '../../data/tripFormOptions'

interface Props {
  value: string
  onChange: (value: string) => void
}

export function CompanionChips({ value, onChange }: Props) {
  return (
    <div className="tc-preference-chips">
      {COMPANION_OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          className={`tc-preference-chip ${value === option.value ? 'tc-preference-chip--active' : ''}`}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
