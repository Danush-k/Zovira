import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { toApiError } from './errors'

/**
 * Shared API client. Requests go to /api/v1 on the same origin; the Vite dev server and the
 * production Nginx both proxy that path to the Spring Boot API, which keeps the refresh-token
 * cookie first-party.
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  withCredentials: true,
  timeout: 20_000,
  headers: {
    Accept: 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
})

interface AuthHooks {
  getToken: () => string | null
  refresh: () => Promise<string | null>
  onSessionExpired: () => void
}

let hooks: AuthHooks | null = null

/** Wired by the auth store at startup; avoids a circular import between the client and the store. */
export function registerAuthHooks(value: AuthHooks) {
  hooks = value
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

http.interceptors.request.use((config) => {
  const token = hooks?.getToken()
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    const isAuthCall = config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && config && !config._retried && !isAuthCall && hooks) {
      config._retried = true
      const token = await hooks.refresh()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        return http(config)
      }
      hooks.onSessionExpired()
      // A stale token should not break public pages: retry reads anonymously.
      if (config.method === 'get') {
        delete config.headers.Authorization
        return http(config)
      }
    }
    return Promise.reject(toApiError(error))
  },
)
