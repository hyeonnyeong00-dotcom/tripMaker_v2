import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { changePassword } from '../../api/auth'
import { extractErrorMessage } from '../../lib/apiError'
import { PasswordStrengthBar } from './PasswordStrengthBar'
import './auth.css'

const MIN_PASSWORD_LENGTH = 8

interface Props {
  onClose: () => void
}

/** 사용자/관리자 헤더 공용 비밀번호 변경 모달 (§7 모달 공통 규칙: 중앙 정렬) */
export function ChangePasswordModal({ onClose }: Props) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('')
  const [confirmTouched, setConfirmTouched] = useState(false)
  const [done, setDone] = useState(false)

  const mutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => setDone(true),
  })

  const newPasswordValid = newPassword.length >= MIN_PASSWORD_LENGTH
  const confirmValid = newPasswordConfirm.length > 0 && newPasswordConfirm === newPassword
  const canSubmit = currentPassword.length > 0 && newPasswordValid && confirmValid

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!canSubmit) return
    mutation.mutate({ current_password: currentPassword, new_password: newPassword })
  }

  return (
    <div className="pw-overlay" onClick={onClose}>
      <div className="pw-modal" onClick={(e) => e.stopPropagation()}>
        <button type="button" className="pw-close" onClick={onClose} aria-label="닫기">
          ✕
        </button>
        <h2 className="pw-title">비밀번호 변경</h2>

        {done ? (
          <div className="pw-done">
            <p className="pw-done-text">비밀번호가 변경되었습니다.</p>
            <button type="button" className="auth-submit" onClick={onClose}>
              확인
            </button>
          </div>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            {mutation.isError && (
              <div className="auth-form-error">
                {extractErrorMessage(mutation.error, '비밀번호 변경에 실패했습니다.')}
              </div>
            )}

            <div className="auth-field">
              <label className="auth-label" htmlFor="pw-current">
                현재 비밀번호
              </label>
              <input
                id="pw-current"
                type="password"
                className="auth-input"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="현재 비밀번호"
                autoComplete="current-password"
              />
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="pw-new">
                새 비밀번호
              </label>
              <input
                id="pw-new"
                type="password"
                className="auth-input"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="8자 이상"
                autoComplete="new-password"
              />
              <PasswordStrengthBar password={newPassword} />
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="pw-new-confirm">
                새 비밀번호 확인
              </label>
              <input
                id="pw-new-confirm"
                type="password"
                className={`auth-input ${confirmTouched && !confirmValid ? 'auth-input--error' : ''}`}
                value={newPasswordConfirm}
                onChange={(e) => setNewPasswordConfirm(e.target.value)}
                onBlur={() => setConfirmTouched(true)}
                placeholder="새 비밀번호 재입력"
                autoComplete="new-password"
              />
              {confirmTouched && !confirmValid && (
                <span className="auth-error-text">새 비밀번호가 일치하지 않습니다.</span>
              )}
            </div>

            <button type="submit" className="auth-submit" disabled={!canSubmit || mutation.isPending}>
              {mutation.isPending ? '변경 중...' : '변경하기'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
