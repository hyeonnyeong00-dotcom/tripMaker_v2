import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import './index.css'
import './styles/tokens.css'
import App from './App.tsx'

const queryClient = new QueryClient({
  defaultOptions: {
    // 4xx(잘못된 요청/권한 없음/없음)는 재시도해도 결과가 같으므로 즉시 실패시키고, 서버/네트워크 오류만 2회까지 재시도.
    // 창 포커스마다 재조회(기본 on)는 불필요한 호출을 유발하므로 끈다.
    queries: {
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (isAxiosError(error) && error.response && error.response.status < 500) return false
        return failureCount < 2
      },
    },
    mutations: {
      retry: false,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
