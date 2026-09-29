/** Shapes returned by the Zovira REST API (v1). */

export type Role = 'CUSTOMER' | 'SELLER' | 'ADMIN'

export interface User {
  id: number
  email: string
  fullName: string
  phone: string | null
  avatarUrl: string | null
  emailVerified: boolean
  roles: Role[]
  sellerStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | null
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: User
}

export interface Session {
  id: number
  userAgent: string | null
  ipAddress: string | null
  createdAt: string
  lastUsedAt: string
  current: boolean
}

export type AddressType = 'HOME' | 'WORK' | 'OTHER'

export interface Address {
  id: number
  fullName: string
  phone: string
  line1: string
  line2: string | null
  landmark: string | null
  city: string
  state: string
  pincode: string
  country: string
  type: AddressType
  isDefault: boolean
}

export interface AddressInput {
  fullName: string
  phone: string
  line1: string
  line2?: string
  landmark?: string
  city: string
  state: string
  pincode: string
  type: AddressType
  makeDefault: boolean
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
