import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updatePromptTemplate } from '../../api/admin'
import { extractErrorMessage } from '../../lib/apiError'
import type { PromptTemplate } from '../../types/admin'

interface Props {
  template: PromptTemplate
}

const NAME_LABELS: Record<string, string> = {
  initial_generation: '최초 생성',
  reorder: '재조정',
}

function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function PromptTemplateCard({ template }: Props) {
  const queryClient = useQueryClient()
  const [content, setContent] = useState(template.content)
  const [error, setError] = useState<string | null>(null)

  const updateMutation = useMutation({
    mutationFn: () => updatePromptTemplate(template.id, content),
    onSuccess: (updated) => {
      setError(null)
      queryClient.setQueryData<PromptTemplate[]>(['admin', 'prompt-templates'], (prev) =>
        prev ? prev.map((t) => (t.id === updated.id ? updated : t)) : prev,
      )
    },
    onError: (err) => setError(extractErrorMessage(err, '템플릿 저장에 실패했습니다.')),
  })

  const isDirty = content !== template.content

  return (
    <div className="ad-template-card">
      <div className="ad-template-head">
        <div>
          <h3 className="ad-template-name">{NAME_LABELS[template.name] ?? template.name}</h3>
          <span className="ad-template-key">{template.name}</span>
        </div>
        <span className={`ad-version-badge ${template.is_active ? 'ad-version-badge--active' : ''}`}>
          v{template.version}
          {template.is_active && ' · 활성'}
        </span>
      </div>

      <textarea
        className="ad-template-textarea"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={8}
      />

      <div className="ad-template-footer">
        <span className="ad-template-updated">최근 수정 {formatDateTime(template.updated_at)}</span>
        <button
          type="button"
          className="ad-template-save"
          disabled={!isDirty || content.trim().length === 0 || updateMutation.isPending}
          onClick={() => updateMutation.mutate()}
        >
          {updateMutation.isPending ? '저장 중...' : '저장'}
        </button>
      </div>

      {error && <p className="ad-error-text">{error}</p>}
    </div>
  )
}
