import { describe, expect, it } from 'vitest'
import { choose, findVariant, initialSelection, optionState } from '../variants'
import type { ProductAttribute, ProductVariant } from '@/types/catalog'

const v = (id: number, colour: string, storage: string, stock: 'IN_STOCK' | 'OUT_OF_STOCK' = 'IN_STOCK', isDefault = false): ProductVariant => ({
  id,
  sku: `SKU-${id}`,
  name: `${colour}, ${storage}`,
  options: { Colour: colour, Storage: storage },
  price: 100,
  mrp: 100,
  discountPercent: 0,
  stockStatus: stock,
  maxPurchasable: stock === 'IN_STOCK' ? 5 : 0,
  lowStockQuantity: null,
  isDefault,
})

const attributes: ProductAttribute[] = [
  { name: 'Colour', options: [{ value: 'Black' }, { value: 'White' }] },
  { name: 'Storage', options: [{ value: '128 GB' }, { value: '256 GB' }] },
]

const variants = [
  v(1, 'Black', '128 GB', 'IN_STOCK', true),
  v(2, 'Black', '256 GB', 'OUT_OF_STOCK'),
  v(3, 'White', '128 GB'),
]

describe('variant selection', () => {
  it('starts from the default in-stock variant', () => {
    expect(initialSelection(variants)).toEqual({ Colour: 'Black', Storage: '128 GB' })
  })

  it('skips an out-of-stock default', () => {
    const list = [v(1, 'Black', '128 GB', 'OUT_OF_STOCK', true), v(2, 'White', '128 GB')]
    expect(initialSelection(list)).toEqual({ Colour: 'White', Storage: '128 GB' })
  })

  it('finds the exact matching variant', () => {
    expect(findVariant(variants, { Colour: 'White', Storage: '128 GB' })?.id).toBe(3)
  })

  it('reports option availability relative to the current selection', () => {
    const selection = { Colour: 'Black', Storage: '128 GB' }
    expect(optionState(variants, selection, 'Storage', '256 GB')).toBe('out-of-stock')
    expect(optionState(variants, { Colour: 'White', Storage: '128 GB' }, 'Storage', '256 GB')).toBe('unavailable')
    expect(optionState(variants, selection, 'Colour', 'White')).toBe('available')
  })

  it('snaps to the closest real combination when the exact one does not exist', () => {
    const next = choose(variants, attributes, { Colour: 'Black', Storage: '256 GB' }, 'Colour', 'White')
    expect(next).toEqual({ Colour: 'White', Storage: '128 GB' })
  })
})
