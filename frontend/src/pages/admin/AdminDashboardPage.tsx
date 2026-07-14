import { useQuery } from '@tanstack/react-query'
import { getDestinationStats, getFlaggedTrips, listPromptTemplates } from '../../api/admin'
import { AdminLayout } from '../../components/admin/AdminLayout'
import { SummaryCards } from '../../components/admin/SummaryCards'
import { FlaggedTripsTable } from '../../components/admin/FlaggedTripsTable'
import { DestinationBarChart } from '../../components/admin/DestinationBarChart'
import { PromptTemplateCard } from '../../components/admin/PromptTemplateCard'
import { extractErrorMessage } from '../../lib/apiError'
import '../../components/admin/admin.css'

export default function AdminDashboardPage() {
  const flaggedQuery = useQuery({ queryKey: ['admin', 'flagged-trips'], queryFn: getFlaggedTrips })
  const statsQuery = useQuery({ queryKey: ['admin', 'stats', 'destinations'], queryFn: getDestinationStats })
  const templatesQuery = useQuery({ queryKey: ['admin', 'prompt-templates'], queryFn: listPromptTemplates })

  const isLoading = flaggedQuery.isLoading || statsQuery.isLoading || templatesQuery.isLoading
  const error = flaggedQuery.error ?? statsQuery.error ?? templatesQuery.error

  return (
    <AdminLayout>
      <div className="ad-shell">
        {isLoading && <p className="ad-status-text">불러오는 중...</p>}

        {error && !isLoading && (
          <p className="ad-status-text">{extractErrorMessage(error, '관리자 데이터를 불러오지 못했습니다.')}</p>
        )}

        {!isLoading && !error && flaggedQuery.data && statsQuery.data && templatesQuery.data && (
          <>
            <SummaryCards
              totalTrips={statsQuery.data.total_trips}
              periodDays={statsQuery.data.period_days}
              flaggedCount={flaggedQuery.data.flagged_count}
              flaggedRatio={flaggedQuery.data.flagged_ratio}
              activeTemplateCount={templatesQuery.data.filter((t) => t.is_active).length}
            />

            <FlaggedTripsTable trips={flaggedQuery.data.flagged_trips} />

            <DestinationBarChart destinations={statsQuery.data.destinations} />

            <section className="ad-section">
              <h2 className="ad-section-title">프롬프트 템플릿</h2>
              <div className="ad-template-list">
                {templatesQuery.data.map((template) => (
                  <PromptTemplateCard key={template.id} template={template} />
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    </AdminLayout>
  )
}
