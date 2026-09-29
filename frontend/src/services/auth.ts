import { http } from './http'
import type { AuthResponse, Session } from '@/types/api'

export const authApi = {
  login: (email: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),

  register: (fullName: string, email: string, password: string) =>
    http.post<AuthResponse>('/auth/register', { fullName, email, password }).then((r) => r.data),

  refresh: () => http.post<AuthResponse>('/auth/refresh').then((r) => r.data),

  logout: () => http.post<void>('/auth/logout').then(() => undefined),

  logoutAll: () => http.post<void>('/auth/logout-all').then(() => undefined),

  verifyEmail: (token: string) => http.post<void>('/auth/verify-email', { token }).then(() => undefined),

  resendVerification: () => http.post<void>('/auth/resend-verification').then(() => undefined),

  forgotPassword: (email: string) => http.post<void>('/auth/forgot-password', { email }).then(() => undefined),

  resetPassword: (token: string, password: string) =>
    http.post<void>('/auth/reset-password', { token, password }).then(() => undefined),

  sessions: () => http.get<Session[]>('/auth/sessions').then((r) => r.data),

  revokeSession: (id: number) => http.delete<void>(`/auth/sessions/${id}`).then(() => undefined),
}
