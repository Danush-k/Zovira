import { http } from './http'
import type { SearchParams, SearchResult, Suggestions } from '@/types/search'

export const searchApi = {
  search: (params: SearchParams, signal?: AbortSignal) =>
    http.get<SearchResult>('/search', { params, signal }).then((r) => r.data),

  suggest: (q: string, signal?: AbortSignal) =>
    http.get<Suggestions>('/search/suggest', { params: { q }, signal }).then((r) => r.data),

  trending: () => http.get<string[]>('/search/trending').then((r) => r.data),

  history: () => http.get<string[]>('/users/me/search-history').then((r) => r.data),

  clearHistory: () => http.delete<void>('/users/me/search-history').then(() => undefined),
}
