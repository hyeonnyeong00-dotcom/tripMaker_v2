import { useQuery } from '@tanstack/react-query'
import { AppHeader } from '../components/layout/AppHeader'
import { TripCard } from '../components/trip/TripCard'
import { EmptyTripState } from '../components/trip/EmptyTripState'
import { listTrips } from '../api/trips'
import { extractErrorMessage } from '../lib/apiError'
import '../components/trip/tripForm.css'
import '../components/trip/tripList.css'

export default function HomePage() {
  const tripsQuery = useQuery({ queryKey: ['trips'], queryFn: listTrips })

  return (
    <>
      <AppHeader />
      <div className="tl-shell">
        <h1 className="tl-heading">내 여행</h1>

        {tripsQuery.isLoading && <p className="tl-status-text">불러오는 중...</p>}

        {tripsQuery.isError && (
          <p className="tl-status-text">{extractErrorMessage(tripsQuery.error, '여행 목록을 불러오지 못했습니다.')}</p>
        )}

        {tripsQuery.data && tripsQuery.data.length === 0 && <EmptyTripState />}

        {tripsQuery.data && tripsQuery.data.length > 0 && (
          <div className="tl-list">
            {tripsQuery.data.map((trip) => (
              <TripCard key={trip.trip_id} trip={trip} />
            ))}
          </div>
        )}
      </div>
    </>
  )
}
