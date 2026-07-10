type Strength = 'weak' | 'medium' | 'strong'

function computeStrength(password: string): Strength {
  if (password.length === 0) return 'weak'

  const varietyCount = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^a-zA-Z0-9]/].filter((pattern) =>
    pattern.test(password),
  ).length

  if (password.length >= 10 && varietyCount >= 3) return 'strong'
  if (password.length >= 8 && varietyCount >= 2) return 'medium'
  return 'weak'
}

const STRENGTH_META: Record<Strength, { label: string; color: string; fill: number }> = {
  weak: { label: '약함', color: 'var(--color-error)', fill: 1 },
  medium: { label: '보통', color: 'var(--color-warning-text)', fill: 2 },
  strong: { label: '강함', color: 'var(--color-modified-text)', fill: 3 },
}

export function PasswordStrengthBar({ password }: { password: string }) {
  if (password.length === 0) return null

  const strength = computeStrength(password)
  const meta = STRENGTH_META[strength]

  return (
    <div style={{ marginTop: 6 }}>
      <div style={{ display: 'flex', gap: 4 }}>
        {[1, 2, 3].map((segment) => (
          <div
            key={segment}
            style={{
              height: 4,
              flex: 1,
              borderRadius: 999,
              backgroundColor: segment <= meta.fill ? meta.color : 'var(--color-border)',
            }}
          />
        ))}
      </div>
      <span style={{ fontSize: 12, color: meta.color }}>{meta.label}</span>
    </div>
  )
}
