import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/services/auth'
import { registerAuthHooks } from '@/services/http'
import type { AuthResponse, Role, User } from '@/types/api'

const SESSION_FLAG = 'zovira.session'
const REFRESH_LOCK = 'zovira-auth-refresh'
/** Refresh this long before the access token expires to avoid a failed request round trip. */
const REFRESH_LEAD_MS = 60_000

function readFlag(): boolean {
  try {
    return localStorage.getItem(SESSION_FLAG) === '1'
  } catch {
    return false
  }
}

function writeFlag(on: boolean) {
  try {
    if (on) localStorage.setItem(SESSION_FLAG, '1')
    else localStorage.removeItem(SESSION_FLAG)
  } catch {
    // storage unavailable; the session still works for this tab
  }
}

/**
 * Access tokens live only in memory. The HttpOnly refresh cookie restores the session on reload,
 * and a non-sensitive flag in localStorage records whether a restore is worth attempting and
 * keeps sign-out in sync across tabs.
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(null)
  const expiresAt = ref<number>(0)
  const initialized = ref(false)

  let refreshInFlight: Promise<string | null> | null = null
  let refreshTimer: ReturnType<typeof setTimeout> | undefined
  let initPromise: Promise<void> | null = null
  const listeners = new Set<(user: User | null) => void>()

  const isAuthenticated = computed(() => !!user.value && !!accessToken.value)
  const firstName = computed(() => user.value?.fullName.split(/\s+/)[0] ?? '')
  const isSeller = computed(() => hasRole('SELLER'))
  const isAdmin = computed(() => hasRole('ADMIN'))

  function hasRole(role: Role) {
    return user.value?.roles.includes(role) ?? false
  }

  function applySession(res: AuthResponse) {
    const changed = user.value?.id !== res.user.id
    user.value = res.user
    accessToken.value = res.accessToken
    expiresAt.value = new Date(res.expiresAt).getTime()
    writeFlag(true)
    scheduleRefresh()
    if (changed) listeners.forEach((fn) => fn(res.user))
  }

  function clearSession() {
    const hadUser = !!user.value
    user.value = null
    accessToken.value = null
    expiresAt.value = 0
    clearTimeout(refreshTimer)
    writeFlag(false)
    if (hadUser) listeners.forEach((fn) => fn(null))
  }

  function scheduleRefresh() {
    clearTimeout(refreshTimer)
    const delay = Math.max(5_000, expiresAt.value - Date.now() - REFRESH_LEAD_MS)
    refreshTimer = setTimeout(() => void refresh(), delay)
  }

  /** Single-flight refresh, serialized across tabs with the Web Locks API where available. */
  function refresh(): Promise<string | null> {
    if (refreshInFlight) return refreshInFlight
    const run = async (): Promise<string | null> => {
      try {
        const res = await authApi.refresh()
        applySession(res)
        return res.accessToken
      } catch {
        clearSession()
        return null
      }
    }
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined
    // LockManager.request resolves to the callback's result; its typings double-wrap the promise.
    const guarded = locks ? (locks.request(REFRESH_LOCK, run) as unknown as Promise<string | null>) : run()
    refreshInFlight = guarded.finally(() => {
      refreshInFlight = null
    })
    return refreshInFlight
  }

  /** Restores a previous session once per page load. Safe to await from route guards. */
  function init(): Promise<void> {
    if (!initPromise) {
      initPromise = (async () => {
        if (readFlag()) await refresh()
        initialized.value = true
      })()
    }
    return initPromise
  }

  async function login(email: string, password: string) {
    applySession(await authApi.login(email, password))
  }

  async function register(fullName: string, email: string, password: string) {
    applySession(await authApi.register(fullName, email, password))
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }

  function setUser(updated: User) {
    user.value = updated
  }

  function onChange(fn: (user: User | null) => void) {
    listeners.add(fn)
    return () => listeners.delete(fn)
  }

  registerAuthHooks({
    getToken: () => accessToken.value,
    refresh,
    onSessionExpired: clearSession,
  })

  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
      if (e.key !== SESSION_FLAG) return
      if (e.newValue === null && user.value) clearSession()
      else if (e.newValue === '1' && !user.value) void refresh()
    })
  }

  return {
    user,
    accessToken,
    initialized,
    isAuthenticated,
    firstName,
    isSeller,
    isAdmin,
    hasRole,
    init,
    login,
    register,
    logout,
    refresh,
    setUser,
    onChange,
  }
})
