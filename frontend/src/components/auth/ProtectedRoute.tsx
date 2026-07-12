import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { getSession } from '../../lib/session'

const HOME_BY_ROLE: Record<'user' | 'admin', string> = {
  user: '/',
  admin: '/admin',
}

export function ProtectedRoute({
  children,
  allowedRoles,
}: {
  children: ReactNode
  allowedRoles: Array<'user' | 'admin'>
}) {
  const session = getSession()
  const adminOnly = allowedRoles.length === 1 && allowedRoles[0] === 'admin'
  if (!session) {
    return <Navigate to={adminOnly ? '/admin/login' : '/login'} replace />
  }
  if (!allowedRoles.includes(session.role)) {
    return <Navigate to={HOME_BY_ROLE[session.role]} replace />
  }
  return children
}
