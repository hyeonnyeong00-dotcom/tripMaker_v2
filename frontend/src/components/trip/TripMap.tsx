import { useEffect, useMemo } from 'react'
import { APIProvider, Map, Marker, useMap, useMapsLibrary } from '@vis.gl/react-google-maps'
import type { Activity } from '../../types/trip'
import { MapErrorBoundary } from './MapErrorBoundary'

interface Props {
  activities: Activity[]
  flagged: boolean
}

const DEFAULT_CENTER = { lat: 37.5665, lng: 126.978 }

function buildMarkerIcon(index: number, core: typeof google.maps.CoreLibrary): google.maps.Icon {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32">
    <circle cx="16" cy="16" r="14" fill="#0062F4" stroke="white" stroke-width="2"/>
    <text x="16" y="21" font-size="14" font-family="sans-serif" font-weight="700" fill="white" text-anchor="middle">${index}</text>
  </svg>`
  return {
    url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    scaledSize: new core.Size(32, 32),
    anchor: new core.Point(16, 16),
  }
}

/** 'core' 라이브러리가 실제로 로드된 뒤에만 Size/Point 생성자를 사용한다(로딩 전 호출 시 런타임 에러). */
function useNumberedMarkerIcons(count: number): (google.maps.Icon | undefined)[] {
  const core = useMapsLibrary('core')
  return useMemo(() => {
    if (!core) return Array.from({ length: count }, () => undefined)
    return Array.from({ length: count }, (_, i) => buildMarkerIcon(i + 1, core))
  }, [core, count])
}

function RoutePolyline({ path, flagged }: { path: google.maps.LatLngLiteral[]; flagged: boolean }) {
  const map = useMap()

  useEffect(() => {
    if (!map || path.length < 2) return

    const polyline = new google.maps.Polyline({
      path,
      strokeColor: flagged ? '#F5A623' : '#0062F4',
      strokeOpacity: flagged ? 0 : 1,
      strokeWeight: 3,
      icons: flagged
        ? [
            {
              icon: { path: 'M 0,-1 0,1', strokeOpacity: 1, scale: 3 },
              offset: '0',
              repeat: '10px',
            },
          ]
        : undefined,
    })
    polyline.setMap(map)

    return () => polyline.setMap(null)
  }, [map, path, flagged])

  return null
}

function ActivityMarkers({ activities }: { activities: Activity[] }) {
  const icons = useNumberedMarkerIcons(activities.length)

  return (
    <>
      {activities.map((activity, index) => (
        <Marker key={activity.id} position={{ lat: activity.lat, lng: activity.lng }} icon={icons[index]} />
      ))}
    </>
  )
}

function FitBounds({ positions }: { positions: google.maps.LatLngLiteral[] }) {
  const map = useMap()

  useEffect(() => {
    if (!map || positions.length === 0) return

    if (positions.length === 1) {
      map.setCenter(positions[0])
      map.setZoom(14)
      return
    }

    const bounds = new google.maps.LatLngBounds()
    positions.forEach((position) => bounds.extend(position))
    map.fitBounds(bounds, 48)
  }, [map, positions])

  return null
}

/** CLAUDE.md 2절: Marker + Polyline만 사용(Directions API 금지) */
export function TripMap({ activities, flagged }: Props) {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined
  const positions = useMemo(() => activities.map((a) => ({ lat: a.lat, lng: a.lng })), [activities])
  const center = positions[0] ?? DEFAULT_CENTER

  return (
    <div className="it-map">
      <MapErrorBoundary>
        <APIProvider apiKey={apiKey ?? ''}>
          <Map defaultCenter={center} defaultZoom={13} gestureHandling="greedy" disableDefaultUI>
            <ActivityMarkers activities={activities} />
            <RoutePolyline path={positions} flagged={flagged} />
            <FitBounds positions={positions} />
          </Map>
        </APIProvider>
      </MapErrorBoundary>
    </div>
  )
}
