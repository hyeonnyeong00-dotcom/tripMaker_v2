import type { ErrorLogItem } from '../../types/admin'

interface Props {
  errors: ErrorLogItem[]
}

/** 표 폭을 아끼려고 "09.17 12:24:22"로 줄여 쓴다(연도 포함 전체 값은 title 툴팁). */
function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function fullDateTime(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString('ko-KR')
}

export function RecentErrorsTable({ errors }: Props) {
  return (
    <section className="ad-section">
      <h2 className="ad-section-title">최근 에러</h2>
      {errors.length === 0 ? (
        <p className="ad-empty-text">기록된 에러가 없습니다.</p>
      ) : (
        <div className="ad-table-wrap">
          <table className="ad-table ad-errors-table">
            <thead>
              <tr>
                <th>시각</th>
                <th>코드</th>
                <th>분류</th>
                <th>메시지</th>
                <th>경로</th>
              </tr>
            </thead>
            <tbody>
              {errors.map((item) => (
                <tr key={item.id}>
                  <td title={fullDateTime(item.created_at)}>{formatDateTime(item.created_at)}</td>
                  <td>{item.error_code}</td>
                  <td>{item.error_category}</td>
                  <td className="ad-table-reason" title={item.message ?? undefined}>
                    {item.message ?? '-'}
                  </td>
                  <td title={item.path ?? undefined}>{item.path ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
