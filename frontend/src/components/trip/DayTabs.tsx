import type { ItineraryDay } from '../../types/trip'

interface Props {
  days: ItineraryDay[]
  selectedDay: number
  onSelect: (day: number) => void
}

export function DayTabs({ days, selectedDay, onSelect }: Props) {
  return (
    <div className="it-day-tabs">
      {days.map((day) => (
        <button
          key={day.day}
          type="button"
          className={`it-day-tab ${day.day === selectedDay ? 'it-day-tab--active' : ''}`}
          onClick={() => onSelect(day.day)}
        >
          Day {day.day}
        </button>
      ))}
    </div>
  )
}
