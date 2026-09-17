import { BrowserRouter, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import HomePage from './pages/HomePage'
import TripCreatePage from './pages/TripCreatePage'
import TripItineraryPage from './pages/TripItineraryPage'
import AdminLoginPage from './pages/admin/AdminLoginPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminTripDetailPage from './pages/admin/AdminTripDetailPage'
import AdminAiUsagePage from './pages/admin/AdminAiUsagePage'
import AdminErrorsPage from './pages/admin/AdminErrorsPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import AdminSettingsPage from './pages/admin/AdminSettingsPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/admin/login" element={<AdminLoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute allowedRoles={['user']}>
              <HomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/trips/new"
          element={
            <ProtectedRoute allowedRoles={['user']}>
              <TripCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/trips/:tripId"
          element={
            <ProtectedRoute allowedRoles={['user']}>
              <TripItineraryPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/trips/:tripId"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminTripDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/ai-usage"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminAiUsagePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/errors"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminErrorsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/admins"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminUsersPage role="admin" title="관리자 관리" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminUsersPage role="user" title="사용자 관리" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/settings"
          element={
            <ProtectedRoute allowedRoles={['admin']}>
              <AdminSettingsPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
