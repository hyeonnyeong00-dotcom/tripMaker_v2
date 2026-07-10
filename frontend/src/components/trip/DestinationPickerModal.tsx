import { useState } from 'react'
import { DOMESTIC, INTERNATIONAL } from '../../data/destinations'

type Tab = '국내' | '해외'

interface Props {
  open: boolean
  onClose: () => void
  onSelect: (destination: string) => void
}

export function DestinationPickerModal({ open, onClose, onSelect }: Props) {
  const [tab, setTab] = useState<Tab>('국내')
  const [detailRegion, setDetailRegion] = useState<string | null>(null)

  if (!open) return null

  const regions = tab === '국내' ? DOMESTIC : INTERNATIONAL
  const detail = detailRegion ? regions.find((r) => r.region === detailRegion) : undefined

  function changeTab(next: Tab) {
    setTab(next)
    setDetailRegion(null)
  }

  function close() {
    setDetailRegion(null)
    onClose()
  }

  function select(name: string) {
    setDetailRegion(null)
    onSelect(name)
  }

  return (
    <div className="dp-overlay" onClick={close}>
      <div className="dp-modal" onClick={(e) => e.stopPropagation()}>
        <div className="dp-header">
          {detail ? (
            <>
              <button type="button" className="dp-back" onClick={() => setDetailRegion(null)} aria-label="뒤로가기">
                ←
              </button>
              <span className="dp-breadcrumb">
                {tab} &gt; {detail.region}
              </span>
            </>
          ) : (
            <span className="dp-title">목적지 선택</span>
          )}
          <button type="button" className="dp-close" onClick={close} aria-label="닫기">
            ✕
          </button>
        </div>

        {!detail ? (
          <div className="dp-body">
            <div className="dp-tabs">
              {(['국내', '해외'] as Tab[]).map((t) => (
                <button
                  key={t}
                  type="button"
                  className={`dp-tab ${tab === t ? 'dp-tab--active' : ''}`}
                  onClick={() => changeTab(t)}
                >
                  {t}
                </button>
              ))}
            </div>
            <ul className="dp-region-list">
              {regions.map((r) => (
                <li key={r.region}>
                  <button type="button" className="dp-region-item" onClick={() => setDetailRegion(r.region)}>
                    {r.region}
                    <span className="dp-region-arrow">›</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        ) : (
          <div className="dp-detail-body">
            {'areas' in detail ? (
              <ul className="dp-area-list">
                {detail.areas.map((area) => (
                  <li key={area}>
                    <button type="button" className="dp-area-item" onClick={() => select(area)}>
                      {area}
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="dp-city-chips">
                {detail.cities.map((city) => (
                  <button key={city} type="button" className="dp-city-chip" onClick={() => select(city)}>
                    {city}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
