import { http } from './http';
export const sellerApi = {
    application: () => http.get('/sell/application').then((r) => r.data || null),
    apply: (body) => http.post('/sell/application', body).then((r) => r.data),
    profile: () => http.get('/seller/profile').then((r) => r.data),
    updateProfile: (body) => http.put('/seller/profile', body).then((r) => r.data),
    dashboard: () => http.get('/seller/dashboard').then((r) => r.data),
    products: (params) => http.get('/seller/products', { params }).then((r) => r.data),
    createProduct: (body) => http.post('/seller/products', body).then((r) => r.data),
    updateProduct: (id, body) => http.put(`/seller/products/${id}`, body).then((r) => r.data),
    setProductStatus: (id, status) => http.patch(`/seller/products/${id}/status`, { status }).then((r) => r.data),
    inventory: (lowOnly = false) => http.get('/seller/inventory', { params: { lowOnly } }).then((r) => r.data),
    updateStock: (rows) => http.put('/seller/inventory', { rows }).then((r) => r.data),
    orders: (filter, page = 0) => http.get('/seller/orders', { params: { filter, page, size: 20 } }).then((r) => r.data),
    updateShipment: (id, body) => http.patch(`/seller/shipments/${id}`, body).then((r) => r.data),
    returns: (openOnly = true, page = 0) => http.get('/seller/returns', { params: { openOnly, page } }).then((r) => r.data),
    decideReturn: (id, decision, note) => http.post(`/seller/returns/${id}/decision`, { decision, note }).then((r) => r.data),
};
