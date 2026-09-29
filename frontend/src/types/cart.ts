export type CartIssue = 'PRICE_INCREASED' | 'PRICE_DROPPED' | 'OUT_OF_STOCK' | 'LIMITED_STOCK' | 'UNAVAILABLE'

export interface CartItem {
  id: number | null
  productId: number
  slug: string
  title: string
  brand: string | null
  imageUrl: string | null
  variantId: number
  variantName: string
  options: Record<string, string>
  sku: string
  quantity: number
  maxQuantity: number
  unitPrice: number
  unitMrp: number
  lineTotal: number
  priceAtAdd: number | null
  stockStatus: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
  purchasable: boolean
  issues: CartIssue[]
  sellerName: string
  codAvailable: boolean
}

export interface CartSummary {
  itemCount: number
  mrpTotal: number
  subtotal: number
  itemDiscount: number
  couponCode: string | null
  couponApplied: boolean
  couponDiscount: number
  couponMessage: string | null
  deliveryOption: 'STANDARD' | 'EXPRESS'
  shippingFee: number
  freeShippingThreshold: number
  amountToFreeShipping: number
  taxIncluded: number
  total: number
  hasIssues: boolean
}

export interface Cart {
  items: CartItem[]
  savedForLater: CartItem[]
  summary: CartSummary
}

export interface CouponOffer {
  code: string
  description: string
  discountType: 'PERCENTAGE' | 'FIXED'
  discountValue: number
  minOrderValue: number
  maxDiscount: number | null
  expiresAt: string | null
  applicable: boolean
  estimatedSaving: number | null
  message: string | null
}

export interface WishlistItem {
  productId: number
  slug: string
  title: string
  brand: string | null
  imageUrl: string | null
  price: number
  mrp: number
  discountPercent: number
  priceAtAdd: number
  priceDrop: number
  inStock: boolean
  available: boolean
  defaultVariantId: number | null
  variantCount: number
  addedAt: string
}

export interface GuestLine {
  variantId: number
  quantity: number
}
