import type { ErrorLogItem } from '../../types/admin'

interface Props {
  errors: ErrorLogItem[]
}

function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

export function RecentErrorsTable({ errors }: Props) {
  return (
    <section className="ad-section">
      <h2 className="ad-section-title">최근 에러</h2>
      {errors.length === 0 ? (
        <p className="ad-empty-text">기록된 에러가 없습니다.</p>
      ) : (
        <div className="ad-table-wrap">
          <table className="ad-table">
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
                  <td>{formatDateTime(item.created_at)}</td>
                  <td>{item.error_code}</td>
                  <td>{item.error_category}</td>
                  <td className="ad-table-reason">{item.message ?? '-'}</td>
                  <td>{item.path ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
