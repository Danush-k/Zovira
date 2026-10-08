import { http } from './http';
export const accountApi = {
    me: () => http.get('/users/me').then((r) => r.data),
    updateProfile: (fullName, phone) => http.patch('/users/me', { fullName, phone: phone || null }).then((r) => r.data),
    changePassword: (currentPassword, newPassword) => http.put('/users/me/password', { currentPassword, newPassword }).then(() => undefined),
    addresses: () => http.get('/users/me/addresses').then((r) => r.data),
    createAddress: (input) => http.post('/users/me/addresses', input).then((r) => r.data),
    updateAddress: (id, input) => http.put(`/users/me/addresses/${id}`, input).then((r) => r.data),
    makeDefaultAddress: (id) => http.post(`/users/me/addresses/${id}/default`).then(() => undefined),
    deleteAddress: (id) => http.delete(`/users/me/addresses/${id}`).then(() => undefined),
};
