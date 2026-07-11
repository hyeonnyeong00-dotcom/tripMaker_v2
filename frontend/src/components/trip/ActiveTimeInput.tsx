import { useState } from 'react'
import { ACTIVE_TIME_PRESETS } from '../../data/tripFormOptions'
import { TimeWheelPicker } from './TimeWheelPicker'

interface Props {
  startTime: string
  endTime: string
  onChange: (start: string, end: string) => void
}

export function ActiveTimeInput({ startTime, endTime, onChange }: Props) {
  const [editing, setEditing] = useState<'start' | 'end' | null>(null)

  const isPreset = (preset: (typeof ACTIVE_TIME_PRESETS)[number]) =>
    preset.start === startTime && preset.end === endTime

  return (
    <div>
      <div className="tc-quick-chips">
        {ACTIVE_TIME_PRESETS.map((preset) => (
          <button
            key={preset.label}
            type="button"
            className={`tc-quick-chip ${isPreset(preset) ? 'tc-quick-chip--active' : ''}`}
            onClick={() => onChange(preset.start, preset.end)}
          >
            {preset.label}
          </button>
        ))}
      </div>

      <div className="tc-date-row">
        <button type="button" className="tc-input tc-time-box" onClick={() => setEditing('start')}>
          {startTime}
        </button>
        <span className="tc-date-sep">–</span>
        <button type="button" className="tc-input tc-time-box" onClick={() => setEditing('end')}>
          {endTime}
        </button>
      </div>

      {editing === 'start' && (
        <TimeWheelPicker
          label="활동 시작 시간"
          value={startTime}
          onConfirm={(next) => onChange(next, endTime)}
          onClose={() => setEditing(null)}
        />
      )}
      {editing === 'end' && (
        <TimeWheelPicker
          label="활동 종료 시간"
          value={endTime}
          onConfirm={(next) => onChange(startTime, next)}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}
