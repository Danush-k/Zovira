import { describe, expect, it } from 'vitest'
import { safeRedirect } from '../redirect'

describe('safeRedirect', () => {
  it('allows same-site relative paths', () => {
    expect(safeRedirect('/account/orders?page=2')).toBe('/account/orders?page=2')
  })

  it.each(['https://evil.example', '//evil.example', '/\\evil.example', 'javascript:alert(1)', 'account'])(
    'rejects %s',
    (value) => {
      expect(safeRedirect(value)).toBe('/')
    },
  )

  it('falls back when the value is missing', () => {
    expect(safeRedirect(undefined, '/cart')).toBe('/cart')
  })
})
