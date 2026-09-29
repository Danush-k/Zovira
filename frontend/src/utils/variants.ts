import type { ProductAttribute, ProductVariant } from '@/types/catalog'

export type Selection = Record<string, string>

export function matches(variant: ProductVariant, selection: Selection): boolean {
  return Object.entries(selection).every(([name, value]) => variant.options[name] === value)
}

/** The variant that exactly matches every selected option, if any. */
export function findVariant(variants: ProductVariant[], selection: Selection): ProductVariant | null {
  return variants.find((v) => matches(v, selection)) ?? null
}

export type OptionState = 'available' | 'out-of-stock' | 'unavailable'

/**
 * How choosing `value` for `attribute` would play out given the other current selections:
 * available, only out-of-stock, or no such combination at all.
 */
export function optionState(
  variants: ProductVariant[],
  selection: Selection,
  attribute: string,
  value: string,
): OptionState {
  const candidate = { ...selection, [attribute]: value }
  const matching = variants.filter((v) => matches(v, candidate))
  if (!matching.length) return 'unavailable'
  return matching.some((v) => v.stockStatus !== 'OUT_OF_STOCK') ? 'available' : 'out-of-stock'
}

/**
 * Applies a choice. If the exact combination doesn't exist, keeps the new value and snaps the
 * other attributes to the closest in-stock variant that has it.
 */
export function choose(
  variants: ProductVariant[],
  attributes: ProductAttribute[],
  selection: Selection,
  attribute: string,
  value: string,
): Selection {
  const next = { ...selection, [attribute]: value }
  if (findVariant(variants, next)) return next
  const withValue = variants.filter((v) => v.options[attribute] === value)
  const best =
    withValue.find((v) => v.stockStatus !== 'OUT_OF_STOCK' && sharedOptions(v, next, attributes) > 0) ??
    withValue.find((v) => v.stockStatus !== 'OUT_OF_STOCK') ??
    withValue[0]
  return best ? { ...best.options } : next
}

function sharedOptions(variant: ProductVariant, selection: Selection, attributes: ProductAttribute[]) {
  return attributes.filter((a) => variant.options[a.name] === selection[a.name]).length
}

export function initialSelection(variants: ProductVariant[], preferredId?: number | null): Selection {
  const preferred =
    variants.find((v) => v.id === preferredId && v.stockStatus !== 'OUT_OF_STOCK') ??
    variants.find((v) => v.isDefault && v.stockStatus !== 'OUT_OF_STOCK') ??
    variants.find((v) => v.stockStatus !== 'OUT_OF_STOCK') ??
    variants[0]
  return preferred ? { ...preferred.options } : {}
}
