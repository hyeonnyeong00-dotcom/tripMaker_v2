import { useQuery } from '@tanstack/react-query'
import { getAiUsageDaily, getAiUsageSummary } from '../../api/admin'
import { AdminLayout } from '../../components/admin/AdminLayout'
import { AiUsageCards } from '../../components/admin/AiUsageCards'
import { AiUsageTrendChart } from '../../components/admin/AiUsageTrendChart'
import { extractErrorMessage } from '../../lib/apiError'
import '../../components/admin/admin.css'

const TREND_DAYS = 14

export default function AdminAiUsagePage() {
  const summaryQuery = useQuery({
    queryKey: ['admin', 'ai-usage', 'summary'],
    queryFn: getAiUsageSummary,
  })
  const dailyQuery = useQuery({
    queryKey: ['admin', 'ai-usage', 'daily', TREND_DAYS],
    queryFn: () => getAiUsageDaily(TREND_DAYS),
  })

  const isLoading = summaryQuery.isLoading || dailyQuery.isLoading
  const error = summaryQuery.error ?? dailyQuery.error

  return (
    <AdminLayout>
      <div className="ad-shell">
        <h1 className="ad-page-title">AI 사용량</h1>

        {isLoading && <p className="ad-status-text">불러오는 중...</p>}

        {error && !isLoading && (
          <p className="ad-status-text">{extractErrorMessage(error, 'AI 사용량 데이터를 불러오지 못했습니다.')}</p>
        )}

        {!isLoading && !error && summaryQuery.data && dailyQuery.data && (
          <>
            <AiUsageCards summary={summaryQuery.data} />
            <AiUsageTrendChart points={dailyQuery.data.points} days={dailyQuery.data.days} />
          </>
        )}
      </div>
    </AdminLayout>
  )
}
