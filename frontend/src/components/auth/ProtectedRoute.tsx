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
  if (!session) {
    return <Navigate to="/login" replace />
  }
  if (!allowedRoles.includes(session.role)) {
    return <Navigate to={HOME_BY_ROLE[session.role]} replace />
  }
  return children
}
