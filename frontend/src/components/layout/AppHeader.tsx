import { useMutation } from '@tanstack/react-query'
import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../../api/auth'
import { clearSession } from '../../lib/session'
import './appHeader.css'

export function AppHeader() {
  const navigate = useNavigate()

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
      <button
        type="button"
        className="app-header-logout"
        onClick={() => logoutMutation.mutate()}
        disabled={logoutMutation.isPending}
      >
        로그아웃
      </button>
    </header>
  )
}
