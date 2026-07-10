import { BrowserRouter, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import HomePage from './pages/HomePage'
import TripCreatePage from './pages/TripCreatePage'
import TripItineraryPage from './pages/TripItineraryPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <HomePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/trips/new"
          element={
            <ProtectedRoute>
              <TripCreatePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/trips/:tripId"
          element={
            <ProtectedRoute>
              <TripItineraryPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default App
