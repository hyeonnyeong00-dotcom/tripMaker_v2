import { BrowserRouter, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import HomePage from './pages/HomePage'
import TripCreatePage from './pages/TripCreatePage'
import TripItineraryPage from './pages/TripItineraryPage'
import AdminLoginPage from './pages/admin/AdminLoginPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminTripDetailPage from './pages/admin/AdminTripDetailPage'
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
      </Routes>
    </BrowserRouter>
  )
}

export default App
