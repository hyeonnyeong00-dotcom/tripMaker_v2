import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getResetPasswordSetting, updateResetPasswordSetting } from '../../api/admin'
import { AdminLayout } from '../../components/admin/AdminLayout'
import { extractErrorMessage } from '../../lib/apiError'
import '../../components/admin/admin.css'

export default function AdminSettingsPage() {
  const queryClient = useQueryClient()
  const [value, setValue] = useState('')
  const [saved, setSaved] = useState(false)

  const settingQuery = useQuery({ queryKey: ['admin', 'settings', 'reset-password'], queryFn: getResetPasswordSetting })

  useEffect(() => {
    if (settingQuery.data) {
      setValue(settingQuery.data.value)
    }
  }, [settingQuery.data])

  const mutation = useMutation({
    mutationFn: () => updateResetPasswordSetting(value),
    onSuccess: (updated) => {
      queryClient.setQueryData(['admin', 'settings', 'reset-password'], updated)
      setSaved(true)
    },
  })

  const isDirty = settingQuery.data !== undefined && value !== settingQuery.data.value

  return (
    <AdminLayout>
      <div className="ad-shell">
        <section className="ad-section">
          <h2 className="ad-section-title">비밀번호 초기화 설정</h2>
          <p className="ad-empty-text">
            관리자가 계정 비밀번호를 초기화하면 아래 값으로 재설정됩니다. (8자 이상)
          </p>

          {settingQuery.isLoading && <p className="ad-status-text">불러오는 중...</p>}
          {settingQuery.isError && (
            <p className="ad-status-text">{extractErrorMessage(settingQuery.error, '설정을 불러오지 못했습니다.')}</p>
          )}

          {settingQuery.data && (
            <div className="ad-setting-card">
              <label className="ad-setting-label" htmlFor="reset-password-value">
                초기화 기본 비밀번호
              </label>
              <div className="ad-setting-row">
                <input
                  id="reset-password-value"
                  className="ad-setting-input"
                  value={value}
                  onChange={(e) => {
                    setValue(e.target.value)
                    setSaved(false)
                  }}
                />
                <button
                  type="button"
                  className="ad-setting-save"
                  disabled={!isDirty || value.trim().length < 8 || mutation.isPending}
                  onClick={() => mutation.mutate()}
                >
                  {mutation.isPending ? '저장 중...' : '저장'}
                </button>
              </div>
              {mutation.isError && (
                <p className="ad-error-text">{extractErrorMessage(mutation.error, '저장에 실패했습니다.')}</p>
              )}
              {saved && <p className="ad-notice-text">저장되었습니다.</p>}
            </div>
          )}
        </section>
      </div>
    </AdminLayout>
  )
}
