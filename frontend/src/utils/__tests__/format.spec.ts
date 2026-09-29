import { describe, expect, it } from 'vitest'
import { discountPercent, formatPrice, pluralize } from '../format'

describe('formatPrice', () => {
  it('formats whole rupees with Indian digit grouping', () => {
    expect(formatPrice(129999)).toBe('₹1,29,999')
  })

  it('keeps paise for fractional amounts', () => {
    expect(formatPrice(49.5)).toBe('₹49.50')
  })

  it('accepts numeric strings from the API', () => {
    expect(formatPrice('2499.00')).toBe('₹2,499')
  })

  it('returns an empty string for missing values', () => {
    expect(formatPrice(null)).toBe('')
    expect(formatPrice(undefined)).toBe('')
  })
})

describe('discountPercent', () => {
  it('rounds to the nearest whole percent', () => {
    expect(discountPercent(749, 999)).toBe(25)
  })

  it('is zero when there is no markdown', () => {
    expect(discountPercent(999, 999)).toBe(0)
    expect(discountPercent(999, 0)).toBe(0)
  })
})

describe('pluralize', () => {
  it('chooses singular or plural form', () => {
    expect(pluralize(1, 'item')).toBe('1 item')
    expect(pluralize(3, 'item')).toBe('3 items')
  })
})
