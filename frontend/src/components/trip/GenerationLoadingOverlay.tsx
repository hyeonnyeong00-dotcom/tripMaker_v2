import { useEffect, useState } from 'react'
import { GENERATION_STEPS } from '../../data/tripFormOptions'

interface Props {
  visible: boolean
  destinationLabel: string
  onComplete: () => void
}

const STEP_INTERVAL_MS = 700

export function GenerationLoadingOverlay({ visible, destinationLabel, onComplete }: Props) {
  const [stepIndex, setStepIndex] = useState(0)

  useEffect(() => {
    if (!visible) {
      setStepIndex(0)
      return
    }

    if (stepIndex >= GENERATION_STEPS.length - 1) {
      const finishTimer = setTimeout(onComplete, STEP_INTERVAL_MS)
      return () => clearTimeout(finishTimer)
    }

    const stepTimer = setTimeout(() => setStepIndex((i) => i + 1), STEP_INTERVAL_MS)
    return () => clearTimeout(stepTimer)
  }, [visible, stepIndex, onComplete])

  if (!visible) return null

  const trimmed = destinationLabel.trim()

  return (
    <div className="tc-loading-overlay">
      <div className="tc-loading-card">
        <p className="tc-loading-title">{trimmed ? `${trimmed} ` : ''}일정을 만들고 있어요</p>
        <ul className="tc-loading-steps">
          {GENERATION_STEPS.map((step, i) => {
            const state = i < stepIndex ? 'done' : i === stepIndex ? 'active' : 'pending'
            return (
              <li key={step} className={`tc-loading-step tc-loading-step--${state}`}>
                <span className="tc-loading-step-icon">{state === 'done' ? '✓' : ''}</span>
                <span>{step}</span>
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}
