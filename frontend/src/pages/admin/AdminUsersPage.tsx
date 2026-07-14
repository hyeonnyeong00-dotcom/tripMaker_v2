import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deleteUser, listUsers, resetUserPassword, updateUserRole } from '../../api/admin'
import { AdminLayout } from '../../components/admin/AdminLayout'
import { extractErrorMessage } from '../../lib/apiError'
import { getSession } from '../../lib/session'
import type { AdminUser } from '../../types/admin'
import '../../components/admin/admin.css'

interface Props {
  role: 'user' | 'admin'
  title: string
}

type DialogAction = 'role' | 'reset' | 'delete'

interface DialogState {
  action: DialogAction
  user: AdminUser
}

function formatDateTime(iso: string | null): string {
  if (!iso) return '-'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function dialogMessage(dialog: DialogState): string {
  const nextRole = dialog.user.role === 'admin' ? 'user' : 'admin'
  switch (dialog.action) {
    case 'role':
      return `'${dialog.user.email}' 계정의 역할을 ${dialog.user.role} → ${nextRole}(으)로 변경할까요?`
    case 'reset':
      return `'${dialog.user.email}' 계정의 비밀번호를 초기화 기본값으로 재설정할까요?`
    case 'delete':
      return `'${dialog.user.email}' 계정을 삭제할까요? 저장된 여행 데이터도 함께 삭제됩니다.`
  }
}

export default function AdminUsersPage({ role, title }: Props) {
  const queryClient = useQueryClient()
  const myEmail = getSession()?.email
  const [dialog, setDialog] = useState<DialogState | null>(null)
  const [dialogError, setDialogError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const usersQuery = useQuery({ queryKey: ['admin', 'users', role], queryFn: () => listUsers(role) })

  const mutation = useMutation({
    mutationFn: async (d: DialogState) => {
      if (d.action === 'role') {
        await updateUserRole(d.user.user_id, d.user.role === 'admin' ? 'user' : 'admin')
      } else if (d.action === 'reset') {
        await resetUserPassword(d.user.user_id)
      } else {
        await deleteUser(d.user.user_id)
      }
      return d
    },
    onSuccess: (d) => {
      setDialog(null)
      setDialogError(null)
      setNotice(
        d.action === 'role'
          ? `'${d.user.email}' 역할이 변경되었습니다.`
          : d.action === 'reset'
            ? `'${d.user.email}' 비밀번호가 초기화되었습니다.`
            : `'${d.user.email}' 계정이 삭제되었습니다.`,
      )
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (error) => setDialogError(extractErrorMessage(error, '요청 처리에 실패했습니다.')),
  })

  function openDialog(action: DialogAction, user: AdminUser) {
    setDialogError(null)
    setNotice(null)
    setDialog({ action, user })
  }

  return (
    <AdminLayout>
      <div className="ad-shell">
        <section className="ad-section">
          <h2 className="ad-section-title">{title}</h2>

          {notice && <p className="ad-notice-text">{notice}</p>}
          {usersQuery.isLoading && <p className="ad-status-text">불러오는 중...</p>}
          {usersQuery.isError && (
            <p className="ad-status-text">{extractErrorMessage(usersQuery.error, '목록을 불러오지 못했습니다.')}</p>
          )}

          {usersQuery.data && usersQuery.data.length === 0 && <p className="ad-empty-text">계정이 없습니다.</p>}

          {usersQuery.data && usersQuery.data.length > 0 && (
            <div className="ad-table-wrap">
              <table className="ad-table">
                <thead>
                  <tr>
                    <th>이메일</th>
                    <th>마지막 로그인</th>
                    <th className="ad-table-actions-col">관리</th>
                  </tr>
                </thead>
                <tbody>
                  {usersQuery.data.map((user) => {
                    const isSelf = user.email === myEmail
                    return (
                      <tr key={user.user_id}>
                        <td>
                          {user.email}
                          {isSelf && <span className="ad-self-badge">본인</span>}
                        </td>
                        <td>{formatDateTime(user.last_login_at)}</td>
                        <td>
                          <div className="ad-row-actions">
                            <button
                              type="button"
                              className="ad-row-btn"
                              disabled={isSelf}
                              title={isSelf ? '본인 계정의 역할은 변경할 수 없습니다' : undefined}
                              onClick={() => openDialog('role', user)}
                            >
                              역할 변경
                            </button>
                            <button type="button" className="ad-row-btn" onClick={() => openDialog('reset', user)}>
                              비밀번호 초기화
                            </button>
                            <button
                              type="button"
                              className="ad-row-btn ad-row-btn--danger"
                              disabled={isSelf}
                              title={isSelf ? '본인 계정은 삭제할 수 없습니다' : undefined}
                              onClick={() => openDialog('delete', user)}
                            >
                              삭제
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      {dialog && (
        <div className="ad-dialog-overlay" onClick={() => setDialog(null)}>
          <div className="ad-dialog" onClick={(e) => e.stopPropagation()}>
            <p className="ad-dialog-text">{dialogMessage(dialog)}</p>
            {dialogError && <p className="ad-error-text">{dialogError}</p>}
            <div className="ad-dialog-actions">
              <button
                type="button"
                className="ad-dialog-btn ad-dialog-btn--cancel"
                onClick={() => setDialog(null)}
                disabled={mutation.isPending}
              >
                취소
              </button>
              <button
                type="button"
                className={`ad-dialog-btn ${dialog.action === 'delete' ? 'ad-dialog-btn--danger' : 'ad-dialog-btn--primary'}`}
                onClick={() => mutation.mutate(dialog)}
                disabled={mutation.isPending}
              >
                {mutation.isPending ? '처리 중...' : '확인'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  )
}
