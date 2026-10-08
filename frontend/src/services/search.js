import { http } from './http';
export const searchApi = {
    search: (params, signal) => http.get('/search', { params, signal }).then((r) => r.data),
    suggest: (q, signal) => http.get('/search/suggest', { params: { q }, signal }).then((r) => r.data),
    trending: () => http.get('/search/trending').then((r) => r.data),
    history: () => http.get('/users/me/search-history').then((r) => r.data),
    clearHistory: () => http.delete('/users/me/search-history').then(() => undefined),
};
