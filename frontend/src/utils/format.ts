const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})

const inrPrecise = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const compact = new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 })

/** Formats a rupee amount. Whole rupees are shown without paise; fractional amounts keep two decimals. */
export function formatPrice(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return ''
  const n = typeof value === 'string' ? Number(value) : value
  if (!Number.isFinite(n)) return ''
  return Number.isInteger(n) ? inr.format(n) : inrPrecise.format(n)
}

export function formatCompact(value: number): string {
  return compact.format(value)
}

export function discountPercent(price: number, mrp: number): number {
  if (!mrp || mrp <= price) return 0
  return Math.round(((mrp - price) / mrp) * 100)
}

export function formatDate(iso: string | Date | null | undefined, opts?: Intl.DateTimeFormatOptions): string {
  if (!iso) return ''
  const d = typeof iso === 'string' ? new Date(iso) : iso
  return d.toLocaleDateString('en-IN', opts ?? { day: 'numeric', month: 'short', year: 'numeric' })
}

export function formatDateTime(iso: string | Date | null | undefined): string {
  if (!iso) return ''
  const d = typeof iso === 'string' ? new Date(iso) : iso
  return d.toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function pluralize(count: number, singular: string, plural = `${singular}s`): string {
  return `${count.toLocaleString('en-IN')} ${count === 1 ? singular : plural}`
}
