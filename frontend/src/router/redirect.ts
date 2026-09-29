/**
 * Only allow same-site relative paths as post-login redirects, preventing open-redirect abuse
 * such as `?redirect=https://evil.example` or `?redirect=//evil.example`.
 */
export function safeRedirect(value: unknown, fallback = '/'): string {
  if (typeof value !== 'string') return fallback
  if (!value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) return fallback
  return value
}
