import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/auth'
import { extractErrorMessage } from '../lib/apiError'
import { AuthHeader } from '../components/auth/AuthHeader'
import { PasswordStrengthBar } from '../components/auth/PasswordStrengthBar'
import '../components/auth/auth.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MIN_PASSWORD_LENGTH = 8

export default function SignupPage() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [emailTouched, setEmailTouched] = useState(false)
  const [passwordTouched, setPasswordTouched] = useState(false)
  const [passwordConfirmTouched, setPasswordConfirmTouched] = useState(false)

  const mutation = useMutation({
    mutationFn: signup,
    onSuccess: (_, variables) => {
      navigate('/login', { state: { signupEmail: variables.email }, replace: true })
    },
  })

  const [duplicateEmail, setDuplicateEmail] = useState<string | null>(null)

  const emailValid = EMAIL_PATTERN.test(email)
  const passwordValid = password.length >= MIN_PASSWORD_LENGTH
  const passwordConfirmValid = passwordConfirm.length > 0 && passwordConfirm === password
  const isDuplicate = duplicateEmail !== null && email === duplicateEmail
  const canSubmit = emailValid && passwordValid && passwordConfirmValid && !isDuplicate

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!canSubmit) return
    mutation.mutate(
      { email, password },
      {
        onError: (error) => {
          if (extractErrorMessage(error, '').includes('이미 사용 중인 이메일')) {
            setDuplicateEmail(email)
          }
        },
      },
    )
  }

  return (
    <div className="auth-shell">
      <AuthHeader headline={'환영해요,\n여행을 시작해볼까요?'} subline="이메일과 비밀번호로 간편하게 가입하세요." />
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {mutation.isError && !isDuplicate && (
          <div className="auth-form-error">
            {extractErrorMessage(mutation.error, '회원가입에 실패했습니다.')}
          </div>
        )}

        <div className="auth-field">
          <label className="auth-label" htmlFor="email">
            이메일
          </label>
          <input
            id="email"
            type="email"
            className={`auth-input ${(emailTouched && !emailValid) || isDuplicate ? 'auth-input--error' : ''}`}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onBlur={() => setEmailTouched(true)}
            placeholder="you@example.com"
            autoComplete="email"
          />
          {emailTouched && !emailValid && (
            <span className="auth-error-text">올바른 이메일 형식을 입력해 주세요.</span>
          )}
          {isDuplicate && <span className="auth-error-text">이미 사용 중인 이메일입니다.</span>}
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
            placeholder="8자 이상"
            autoComplete="new-password"
          />
          <PasswordStrengthBar password={password} />
          {passwordTouched && !passwordValid && (
            <span className="auth-error-text">비밀번호는 8자 이상이어야 합니다.</span>
          )}
        </div>

        <div className="auth-field">
          <label className="auth-label" htmlFor="passwordConfirm">
            비밀번호 확인
          </label>
          <input
            id="passwordConfirm"
            type="password"
            className={`auth-input ${passwordConfirmTouched && !passwordConfirmValid ? 'auth-input--error' : ''}`}
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            onBlur={() => setPasswordConfirmTouched(true)}
            placeholder="비밀번호 다시 입력"
            autoComplete="new-password"
          />
          {passwordConfirmTouched && !passwordConfirmValid && (
            <span className="auth-error-text">비밀번호가 일치하지 않습니다.</span>
          )}
        </div>

        <button type="submit" className="auth-submit" disabled={!canSubmit || mutation.isPending}>
          {mutation.isPending ? '가입 중...' : '회원가입'}
        </button>
      </form>

      <div className="auth-switch">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </div>
    </div>
  )
}
