// frontend/src/lib/geo.ts
// Directions API를 쓸 수 없으므로(CLAUDE.md 2절) 좌표 간 직선거리로 이동시간을 추정한다.

const EARTH_RADIUS_KM = 6371
const ASSUMED_SPEED_KMH = 25

export function haversineDistanceKm(
  a: { lat: number; lng: number },
  b: { lat: number; lng: number },
): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180
  const dLat = toRad(b.lat - a.lat)
  const dLng = toRad(b.lng - a.lng)
  const lat1 = toRad(a.lat)
  const lat2 = toRad(b.lat)

  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h))
}

export function estimateTravelMinutes(a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
  const distanceKm = haversineDistanceKm(a, b)
  const minutes = (distanceKm / ASSUMED_SPEED_KMH) * 60
  return Math.max(5, Math.round(minutes / 5) * 5)
}
