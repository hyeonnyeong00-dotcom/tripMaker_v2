import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../../api/auth'
import { clearSession } from '../../lib/session'
import { ChangePasswordModal } from '../auth/ChangePasswordModal'
import './appHeader.css'

export function AppHeader() {
  const navigate = useNavigate()
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession()
      navigate('/login', { replace: true })
    },
  })

  return (
    <header className="app-header">
      <nav className="app-header-nav">
        <NavLink to="/" end className={({ isActive }) => `app-header-link ${isActive ? 'app-header-link--active' : ''}`}>
          내 여행
        </NavLink>
        <NavLink
          to="/trips/new"
          className={({ isActive }) => `app-header-link ${isActive ? 'app-header-link--active' : ''}`}
        >
          새 일정
        </NavLink>
      </nav>
      <div className="app-header-actions">
        <button type="button" className="app-header-logout" onClick={() => setPasswordModalOpen(true)}>
          비밀번호 변경
        </button>
        <button
          type="button"
          className="app-header-logout"
          onClick={() => logoutMutation.mutate()}
          disabled={logoutMutation.isPending}
        >
          로그아웃
        </button>
      </div>

      {passwordModalOpen && <ChangePasswordModal onClose={() => setPasswordModalOpen(false)} />}
    </header>
  )
}
