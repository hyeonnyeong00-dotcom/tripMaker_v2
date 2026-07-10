import { PREFERENCE_OPTIONS } from '../../data/tripFormOptions'

interface Props {
  selected: string[]
  onChange: (next: string[]) => void
}

export function PreferenceChips({ selected, onChange }: Props) {
  function toggle(option: string) {
    if (selected.includes(option)) {
      onChange(selected.filter((item) => item !== option))
    } else {
      onChange([...selected, option])
    }
  }

  return (
    <div>
      <div className="tc-preference-header">
        <span className="tc-preference-count">{selected.length}개 선택됨</span>
      </div>
      <div className="tc-preference-chips">
        {PREFERENCE_OPTIONS.map((option) => (
          <button
            key={option}
            type="button"
            className={`tc-preference-chip ${selected.includes(option) ? 'tc-preference-chip--active' : ''}`}
            onClick={() => toggle(option)}
          >
            {option}
          </button>
        ))}
      </div>
    </div>
  )
}
