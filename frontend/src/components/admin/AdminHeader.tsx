import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { logout } from '../../api/auth'
import { clearSession } from '../../lib/session'
import './admin.css'

/** 5.5-a 완전 분리형: 일반 사용자 메뉴(내 여행/새 일정)는 절대 노출하지 않는다 */
export function AdminHeader() {
  const navigate = useNavigate()

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession()
      navigate('/admin/login', { replace: true })
    },
  })

  return (
    <header className="ad-header">
      <span className="ad-header-title">관리자 대시보드</span>
      <button
        type="button"
        className="ad-header-logout"
        onClick={() => logoutMutation.mutate()}
        disabled={logoutMutation.isPending}
      >
        로그아웃
      </button>
    </header>
  )
}
