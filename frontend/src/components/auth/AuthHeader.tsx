export function AuthHeader({ headline, subline }: { headline: string; subline: string }) {
  return (
    <>
      <div className="auth-brand">
        <span className="auth-brand-mark">✈</span>
        <span className="auth-brand-name">TripMaker</span>
      </div>
      <h1 className="auth-headline">{headline}</h1>
      <p className="auth-subline">{subline}</p>
    </>
  )
}
