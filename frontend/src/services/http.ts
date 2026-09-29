import axios from 'axios'

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
