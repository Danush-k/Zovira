import type { Address } from './api'
import type { Cart } from './cart'

export type PaymentMethod = 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET' | 'COD'
export type DeliveryOption = 'STANDARD' | 'EXPRESS'

export interface CheckoutPreview {
  cart: Cart
  standardDeliveryDate: string | null
  expressDeliveryDate: string | null
  expressAvailable: boolean
  serviceable: boolean
  codAvailable: boolean
  codUnavailableReason: string | null
  codFee: number
  emailVerified: boolean
}

export interface PaymentIntent {
  paymentId: number
  provider: 'RAZORPAY' | 'SANDBOX'
  providerOrderId: string
  keyId: string | null
  amount: number
  amountInPaise: number
  currency: string
  orderNumber: string
  customerName: string
  customerEmail: string
  customerPhone: string
}

export interface PlaceOrderResponse {
  orderNumber: string
  status: string
  paymentRequired: boolean
  payment: PaymentIntent | null
}

export interface PaymentResult {
  orderNumber: string
  orderStatus: string
  paymentStatus: string
}

export interface OrderItem {
  id: number
  productId: number
  productSlug: string
  title: string
  variantName: string | null
  sku: string
  imageUrl: string | null
  unitPrice: number
  unitMrp: number
  quantity: number
  lineTotal: number
  couponDiscount: number
  status: 'ACTIVE' | 'CANCELLED' | 'RETURN_REQUESTED' | 'RETURNED'
  sellerName: string
  shipmentNumber: string | null
  returnable: boolean
  returnableUntil: string | null
  reviewable: boolean
}

export interface Shipment {
  id: number
  shipmentNumber: string
  status: string
  sellerName: string
  carrier: string | null
  trackingNumber: string | null
  estimatedDeliveryDate: string | null
  shippedAt: string | null
  deliveredAt: string | null
  events: { status: string; location: string | null; description: string; at: string }[]
  itemIds: number[]
}

export interface OrderDetail {
  orderNumber: string
  status: string
  paymentStatus: string
  paymentMethod: PaymentMethod
  deliveryOption: DeliveryOption
  shippingAddress: Address
  items: OrderItem[]
  shipments: Shipment[]
  timeline: { status: string; label: string; note: string | null; at: string }[]
  mrpTotal: number
  subtotal: number
  couponCode: string | null
  couponDiscount: number
  shippingFee: number
  codFee: number
  taxAmount: number
  totalAmount: number
  refundedAmount: number
  estimatedDeliveryDate: string | null
  placedAt: string
  deliveredAt: string | null
  cancelledAt: string | null
  cancelReason: string | null
  cancellable: boolean
  paymentPending: boolean
}

export interface OrderSummary {
  orderNumber: string
  status: string
  paymentStatus: string
  paymentMethod: PaymentMethod
  totalAmount: number
  itemCount: number
  placedAt: string
  estimatedDeliveryDate: string | null
  deliveredAt: string | null
  thumbnails: string[]
  firstItemTitle: string | null
}
