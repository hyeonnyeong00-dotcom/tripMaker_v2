import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { AdminHeader } from './AdminHeader'
import './admin.css'

const MENU = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/ai-usage', label: 'AI 사용량', end: false },
  { to: '/admin/errors', label: '에러 모니터링', end: false },
  { to: '/admin/admins', label: '관리자 관리', end: false },
  { to: '/admin/users', label: '사용자 관리', end: false },
  { to: '/admin/settings', label: '비밀번호 초기화 설정', end: false },
]

export function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <AdminHeader />
      <div className="ad-layout">
        <nav className="ad-sidebar">
          {MENU.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `ad-sidebar-link ${isActive ? 'ad-sidebar-link--active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <main className="ad-main">{children}</main>
      </div>
    </>
  )
}
