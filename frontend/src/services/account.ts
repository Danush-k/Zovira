import { http } from './http'
import type { Address, AddressInput, User } from '@/types/api'

export const accountApi = {
  me: () => http.get<User>('/users/me').then((r) => r.data),

  updateProfile: (fullName: string, phone: string | null) =>
    http.patch<User>('/users/me', { fullName, phone: phone || null }).then((r) => r.data),

  changePassword: (currentPassword: string, newPassword: string) =>
    http.put<void>('/users/me/password', { currentPassword, newPassword }).then(() => undefined),

  addresses: () => http.get<Address[]>('/users/me/addresses').then((r) => r.data),

  createAddress: (input: AddressInput) => http.post<Address>('/users/me/addresses', input).then((r) => r.data),

  updateAddress: (id: number, input: AddressInput) =>
    http.put<Address>(`/users/me/addresses/${id}`, input).then((r) => r.data),

  makeDefaultAddress: (id: number) => http.post<void>(`/users/me/addresses/${id}/default`).then(() => undefined),

  deleteAddress: (id: number) => http.delete<void>(`/users/me/addresses/${id}`).then(() => undefined),
}
