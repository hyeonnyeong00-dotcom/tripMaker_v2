import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
}

/** 지도 로드 실패(키 누락/네트워크 등)가 페이지 전체를 무너뜨리지 않도록 격리한다. */
export class MapErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('TripMap 렌더링 실패:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="it-map-fallback">
          <p>지도를 불러오지 못했어요. Google Maps API 키 설정을 확인해주세요.</p>
        </div>
      )
    }
    return this.props.children
  }
}
