import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { saveSession } from '../../lib/session'
import { extractErrorMessage } from '../../lib/apiError'
import '../../components/auth/auth.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** 5.5-a 완전 분리형: 관리자 전용 로그인. 사용자 화면과 구분되는 어두운 배경 + 관리자 배지 */
export default function AdminLoginPage() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [roleError, setRoleError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (auth) => {
      if (auth.role !== 'admin') {
        setRoleError('관리자 계정이 아닙니다. 일반 사용자 로그인을 이용해 주세요.')
        return
      }
      saveSession(auth)
      navigate('/admin', { replace: true })
    },
  })

  const canSubmit = EMAIL_PATTERN.test(email) && password.length > 0

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!canSubmit) return
    setRoleError(null)
    mutation.mutate({ email, password })
  }

  return (
    <div className="auth-shell auth-shell--admin">
      <div className="auth-admin-card">
        <div className="auth-brand">
          <span className="auth-brand-mark">✈</span>
          <span className="auth-brand-name">TripMaker Admin</span>
          <span className="auth-admin-badge">관리자 전용</span>
        </div>
        <h1 className="auth-headline">관리자 로그인</h1>
        <p className="auth-subline">운영자 계정으로만 로그인할 수 있어요.</p>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          {mutation.isError && (
            <div className="auth-form-error">
              {extractErrorMessage(mutation.error, '로그인에 실패했습니다.')}
            </div>
          )}

          {roleError && (
            <div className="auth-form-error">
              {roleError} <Link to="/login">사용자 로그인으로 이동</Link>
            </div>
          )}

          <div className="auth-field">
            <label className="auth-label" htmlFor="admin-email">
              이메일
            </label>
            <input
              id="admin-email"
              type="email"
              className="auth-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@example.com"
              autoComplete="email"
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="admin-password">
              비밀번호
            </label>
            <input
              id="admin-password"
              type="password"
              className="auth-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
              autoComplete="current-password"
            />
          </div>

          <button type="submit" className="auth-submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? '로그인 중...' : '관리자 로그인'}
          </button>
        </form>

        <div className="auth-switch">
          일반 사용자이신가요? <Link to="/login">사용자 로그인</Link>
        </div>
      </div>
    </div>
  )
}
