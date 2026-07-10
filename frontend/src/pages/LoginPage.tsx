import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { saveSession } from '../lib/session'
import { extractErrorMessage } from '../lib/apiError'
import { AuthHeader } from '../components/auth/AuthHeader'
import '../components/auth/auth.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation() as { state?: { signupEmail?: string } }

  const [email, setEmail] = useState(location.state?.signupEmail ?? '')
  const [password, setPassword] = useState('')
  const [emailTouched, setEmailTouched] = useState(false)
  const [passwordTouched, setPasswordTouched] = useState(false)

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (auth) => {
      saveSession(auth)
      navigate('/', { replace: true })
    },
  })

  const emailValid = EMAIL_PATTERN.test(email)
  const passwordValid = password.length > 0
  const canSubmit = emailValid && passwordValid

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!canSubmit) return
    mutation.mutate({ email, password })
  }

  return (
    <div className="auth-shell">
      <AuthHeader headline={'다시 만나서 반가워요'} subline="로그인하고 여행 일정을 이어가 보세요." />
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {mutation.isError && (
          <div className="auth-form-error">
            {extractErrorMessage(mutation.error, '로그인에 실패했습니다.')}
          </div>
        )}

        <div className="auth-field">
          <label className="auth-label" htmlFor="email">
            이메일
          </label>
          <input
            id="email"
            type="email"
            className={`auth-input ${emailTouched && !emailValid ? 'auth-input--error' : ''}`}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onBlur={() => setEmailTouched(true)}
            placeholder="you@example.com"
            autoComplete="email"
          />
          {emailTouched && !emailValid && (
            <span className="auth-error-text">올바른 이메일 형식을 입력해 주세요.</span>
          )}
        </div>

        <div className="auth-field">
          <label className="auth-label" htmlFor="password">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            className={`auth-input ${passwordTouched && !passwordValid ? 'auth-input--error' : ''}`}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={() => setPasswordTouched(true)}
            placeholder="비밀번호"
            autoComplete="current-password"
          />
          {passwordTouched && !passwordValid && (
            <span className="auth-error-text">비밀번호를 입력해 주세요.</span>
          )}
        </div>

        <button type="submit" className="auth-submit" disabled={!canSubmit || mutation.isPending}>
          {mutation.isPending ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <div className="auth-switch">
        아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </div>
    </div>
  )
}
