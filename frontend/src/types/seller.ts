export interface SellerProfile {
  id: number
  storeName: string
  slug: string
  description: string | null
  logoUrl: string | null
  gstin: string | null
  supportEmail: string | null
  supportPhone: string | null
  pickupLine1: string | null
  pickupCity: string | null
  pickupState: string | null
  pickupPincode: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'
  statusReason: string | null
  ratingAverage: number
  ratingCount: number
  approvedAt: string | null
  createdAt: string
}

export interface SellerApplication {
  storeName: string
  description?: string
  gstin?: string
  supportEmail: string
  supportPhone: string
  pickupLine1: string
  pickupCity: string
  pickupState: string
  pickupPincode: string
}

export interface SellerProductSummary {
  id: number
  slug: string
  title: string
  imageUrl: string | null
  categoryName: string
  brandName: string | null
  status: 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'
  price: number
  mrp: number
  totalStock: number
  variantCount: number
  lowStock: boolean
  ratingAverage: number
  ratingCount: number
  soldCount: number
  updatedAt: string
}

export interface InventoryRow {
  variantId: number
  productId: number
  productTitle: string
  productSlug: string
  imageUrl: string | null
  variantName: string
  options: Record<string, string>
  sku: string
  price: number
  available: number
  reserved: number
  lowStockThreshold: number
  active: boolean
}

export interface SellerOrderRow {
  shipmentId: number
  shipmentNumber: string
  orderNumber: string
  status: string
  orderStatus: string
  paymentMethod: string
  paymentStatus: string
  customerName: string
  city: string
  state: string
  pincode: string
  estimatedDeliveryDate: string | null
  carrier: string | null
  trackingNumber: string | null
  placedAt: string
  itemsTotal: number
  items: { id: number; title: string; variantName: string | null; sku: string; imageUrl: string | null; quantity: number; lineTotal: number; status: string }[]
}

export interface SellerDashboard {
  metrics: {
    revenue30d: number
    revenuePrevious30d: number
    orders30d: number
    ordersPrevious30d: number
    unitsSold30d: number
    averageOrderValue: number
    pendingShipments: number
    openReturns: number
    activeProducts: number
    outOfStock: number
    lifetimeRevenue: number
    ratingAverage: number
    ratingCount: number
  }
  revenueSeries: { date: string; revenue: number; orders: number }[]
  topProducts: { productId: number; title: string; slug: string; imageUrl: string | null; units: number; revenue: number }[]
  lowStock: InventoryRow[]
  recentOrders: SellerOrderRow[]
}

export interface VariantInput {
  id?: number | null
  sku?: string
  name?: string
  options: Record<string, string>
  price: number
  mrp: number
  stock: number
  weightGrams?: number | null
  isDefault: boolean
  active: boolean
}

export interface SellerProductInput {
  title: string
  categoryId: number | null
  brandId?: number | null
  newBrandName?: string
  shortDescription?: string
  description?: string
  highlights?: string[]
  tags?: string
  warranty?: string
  returnable: boolean
  returnWindowDays: number
  codAvailable: boolean
  modelUrl?: string
  arModelUrl?: string
  attributes?: { name: string; options: { value: string; swatch?: string }[] }[]
  variants: VariantInput[]
  images?: { url: string; altText?: string; variantOption?: string }[]
  specifications?: { group?: string; name: string; value: string }[]
  publish: boolean
}
