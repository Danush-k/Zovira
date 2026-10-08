import { http } from './http';
export const catalogApi = {
    home: () => http.get('/home').then((r) => r.data),
    categories: () => http.get('/categories').then((r) => r.data),
    brands: () => http.get('/brands').then((r) => r.data),
    brand: (slug) => http.get(`/brands/${encodeURIComponent(slug)}`).then((r) => r.data),
    product: (slug) => http.get(`/products/${encodeURIComponent(slug)}`).then((r) => r.data),
    compare: (slugs) => http.get('/products/compare', { params: { slugs: slugs.join(',') } }).then((r) => r.data),
    store: (slug) => http.get(`/stores/${encodeURIComponent(slug)}`).then((r) => r.data),
    deliveryEstimate: (pincode) => http.get('/delivery/estimate', { params: { pincode } }).then((r) => r.data),
    settings: () => http.get('/settings/public').then((r) => r.data),
};
