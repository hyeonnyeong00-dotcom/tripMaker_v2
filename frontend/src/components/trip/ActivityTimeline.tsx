import { DndContext, closestCenter, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import type { DragEndEvent } from '@dnd-kit/core'
import { SortableContext, verticalListSortingStrategy, arrayMove, useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import type { Activity } from '../../types/trip'
import { estimateTravelMinutes } from '../../lib/geo'

interface Props {
  activities: Activity[]
  onReorder: (next: Activity[]) => void
}

function formatCost(cost: number | null): string {
  if (cost === null || cost === 0) return '무료'
  return `${cost.toLocaleString()}원`
}

interface CardProps {
  activity: Activity
  index: number
  isLast: boolean
}

function SortableActivityCard({ activity, index, isLast }: CardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: activity.id,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`it-timeline-row ${isDragging ? 'it-timeline-row--dragging' : ''}`}
      {...attributes}
      {...listeners}
    >
      <div className="it-timeline-rail">
        <span className="it-timeline-time">{activity.time ?? '--:--'}</span>
        <span className="it-timeline-badge">{index + 1}</span>
        {!isLast && <span className="it-timeline-line" />}
      </div>
      <div className="it-activity-card">
        <div className="it-activity-card-head">
          <h3 className="it-activity-title">{activity.title}</h3>
          <span className="it-activity-category">{activity.category}</span>
        </div>
        {activity.description && <p className="it-activity-desc">{activity.description}</p>}
        <div className="it-activity-meta">
          {activity.duration_minutes != null && <span>⏱ {activity.duration_minutes}분</span>}
          <span>💰 {formatCost(activity.estimated_cost)}</span>
        </div>
        {activity.tips && <p className="it-activity-tips">{activity.tips}</p>}
      </div>
    </div>
  )
}

export function ActivityTimeline({ activities, onReorder }: Props) {
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    if (!over || active.id === over.id) return
    const oldIndex = activities.findIndex((a) => a.id === active.id)
    const newIndex = activities.findIndex((a) => a.id === over.id)
    onReorder(arrayMove(activities, oldIndex, newIndex))
  }

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={activities.map((a) => a.id)} strategy={verticalListSortingStrategy}>
        <div className="it-timeline">
          {activities.map((activity, index) => (
            <div key={activity.id}>
              <SortableActivityCard activity={activity} index={index} isLast={index === activities.length - 1} />
              {index < activities.length - 1 && (
                <div className="it-travel-gap">
                  🚕 약 {estimateTravelMinutes(activity, activities[index + 1])}분
                </div>
              )}
            </div>
          ))}
        </div>
      </SortableContext>
    </DndContext>
  )
}
