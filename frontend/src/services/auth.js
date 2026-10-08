import { http } from './http';
export const authApi = {
    login: (email, password) => http.post('/auth/login', { email, password }).then((r) => r.data),
    register: (fullName, email, password) => http.post('/auth/register', { fullName, email, password }).then((r) => r.data),
    refresh: () => http.post('/auth/refresh').then((r) => r.data),
    logout: () => http.post('/auth/logout').then(() => undefined),
    logoutAll: () => http.post('/auth/logout-all').then(() => undefined),
    verifyEmail: (token) => http.post('/auth/verify-email', { token }).then(() => undefined),
    resendVerification: () => http.post('/auth/resend-verification').then(() => undefined),
    forgotPassword: (email) => http.post('/auth/forgot-password', { email }).then(() => undefined),
    resetPassword: (token, password) => http.post('/auth/reset-password', { token, password }).then(() => undefined),
    sessions: () => http.get('/auth/sessions').then((r) => r.data),
    revokeSession: (id) => http.delete(`/auth/sessions/${id}`).then(() => undefined),
};
