import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { getSession } from '../../lib/session'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const session = getSession()
  if (!session) {
    return <Navigate to="/login" replace />
  }
  return children
}
