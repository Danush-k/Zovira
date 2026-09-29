export interface CategoryNode {
  id: number
  name: string
  slug: string
  icon: string | null
  imageUrl: string | null
  description: string | null
  children: CategoryNode[]
}

export interface CategoryRef {
  id: number
  name: string
  slug: string
}

export interface BrandRef {
  id: number
  name: string
  slug: string
}

export interface Brand {
  id: number
  name: string
  slug: string
  logoUrl: string | null
  description: string | null
  featured: boolean
  imageUrl: string | null
  productCount: number
}

export interface ProductSummary {
  id: number
  slug: string
  title: string
  brand: string | null
  imageUrl: string | null
  price: number
  mrp: number
  discountPercent: number
  ratingAverage: number
  ratingCount: number
  inStock: boolean
  has3dModel: boolean
  defaultVariantId: number | null
  variantCount: number
}

export interface AttributeOption {
  value: string
  swatch?: string
  modelVariant?: string
}

export interface ProductAttribute {
  name: string
  options: AttributeOption[]
}

export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'

export interface ProductVariant {
  id: number
  sku: string
  name: string
  options: Record<string, string>
  price: number
  mrp: number
  discountPercent: number
  stockStatus: StockStatus
  maxPurchasable: number
  lowStockQuantity: number | null
  isDefault: boolean
}

export interface SellerSummary {
  id: number
  storeName: string
  slug: string
  ratingAverage: number
  ratingCount: number
  city: string | null
  state: string | null
}

export interface SpecGroup {
  group: string
  items: { name: string; value: string }[]
}

export interface ProductDetail {
  id: number
  slug: string
  title: string
  shortDescription: string | null
  description: string | null
  highlights: string[]
  brand: BrandRef | null
  category: CategoryRef
  breadcrumbs: CategoryRef[]
  seller: SellerSummary
  images: { id: number; url: string; altText: string | null; variantId: number | null }[]
  attributes: ProductAttribute[]
  variants: ProductVariant[]
  defaultVariantId: number | null
  specifications: SpecGroup[]
  ratingAverage: number
  ratingCount: number
  warranty: string | null
  returnable: boolean
  returnWindowDays: number
  codAvailable: boolean
  taxRate: number
  model: { modelUrl: string; arModelUrl: string | null; posterUrl: string | null } | null
  status: 'ACTIVE' | 'INACTIVE'
  tags: string[]
}

export interface HeroSlide {
  eyebrow: string
  title: string
  subtitle: string
  ctaLabel: string
  ctaLink: string
  imageUrl: string
  tone: 'sand' | 'teal' | 'lilac' | 'mint'
}

export interface HomeData {
  hero: HeroSlide[]
  categories: CategoryNode[]
  deals: ProductSummary[]
  trending: ProductSummary[]
  bestSellers: ProductSummary[]
  newArrivals: ProductSummary[]
  topRated: ProductSummary[]
  immersive: ProductSummary[]
  brands: Brand[]
}

export interface DeliveryOptionEstimate {
  available: boolean
  date: string | null
  days: number
  fee: number
  freeAbove: number | null
}

export interface DeliveryEstimate {
  pincode: string
  serviceable: boolean
  zone: string
  standard: DeliveryOptionEstimate | null
  express: DeliveryOptionEstimate | null
  codAvailable: boolean
}

export interface PlatformSettings {
  freeShippingThreshold: number
  standardShippingFee: number
  expressShippingFee: number
  standardDeliveryDays: number
  expressDeliveryDays: number
  codEnabled: boolean
  codFee: number
  codMaxOrder: number
  maxQuantityPerItem: number
  reviewsAutoPublish: boolean
}

export interface Store {
  seller: SellerSummary
  description: string | null
  logoUrl: string | null
  memberSince: string
  productCount: number
}
