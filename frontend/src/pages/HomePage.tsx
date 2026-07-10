import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { logout, me } from '../api/auth'
import { clearSession } from '../lib/session'
import '../components/auth/auth.css'

export default function HomePage() {
  const navigate = useNavigate()
  const meQuery = useQuery({ queryKey: ['auth', 'me'], queryFn: me })

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession()
      navigate('/login', { replace: true })
    },
  })

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 24px' }}>
      <h1 style={{ fontSize: 20, marginBottom: 16 }}>로그인 완료</h1>
      <p style={{ color: 'var(--color-text-sub)', marginBottom: 24 }}>
        {meQuery.isLoading && '불러오는 중...'}
        {meQuery.isError && '사용자 정보를 불러오지 못했습니다.'}
        {meQuery.data && `${meQuery.data.email} (${meQuery.data.role})님으로 로그인됨. 일정 생성 화면은 다음 마일스톤에서 구현됩니다.`}
      </p>
      <button
        type="button"
        className="auth-submit"
        onClick={() => logoutMutation.mutate()}
        disabled={logoutMutation.isPending}
      >
        로그아웃
      </button>
    </div>
  )
}
