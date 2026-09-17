import { useQuery } from '@tanstack/react-query'
import { getErrorSummary, getRecentErrors } from '../../api/admin'
import { AdminLayout } from '../../components/admin/AdminLayout'
import { ErrorCodeBarChart } from '../../components/admin/ErrorCodeBarChart'
import { RecentErrorsTable } from '../../components/admin/RecentErrorsTable'
import { extractErrorMessage } from '../../lib/apiError'
import '../../components/admin/admin.css'

const RECENT_LIMIT = 50

export default function AdminErrorsPage() {
  const summaryQuery = useQuery({
    queryKey: ['admin', 'errors', 'summary'],
    queryFn: getErrorSummary,
  })
  const recentQuery = useQuery({
    queryKey: ['admin', 'errors', 'recent', RECENT_LIMIT],
    queryFn: () => getRecentErrors(RECENT_LIMIT),
  })

  const isLoading = summaryQuery.isLoading || recentQuery.isLoading
  const error = summaryQuery.error ?? recentQuery.error

  return (
    <AdminLayout>
      <div className="ad-shell">
        <h1 className="ad-page-title">에러 모니터링</h1>

        {isLoading && <p className="ad-status-text">불러오는 중...</p>}

        {error && !isLoading && (
          <p className="ad-status-text">{extractErrorMessage(error, '에러 데이터를 불러오지 못했습니다.')}</p>
        )}

        {!isLoading && !error && summaryQuery.data && recentQuery.data && (
          <>
            <ErrorCodeBarChart items={summaryQuery.data.items} totalErrors={summaryQuery.data.total_errors} />
            <RecentErrorsTable errors={recentQuery.data.errors} />
          </>
        )}
      </div>
    </AdminLayout>
  )
}
