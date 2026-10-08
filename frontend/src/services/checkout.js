import { http } from './http';
export const checkoutApi = {
    preview: (input) => http.post('/checkout/preview', input).then((r) => r.data),
    place: (input, idempotencyKey) => http.post('/orders', input, { headers: { 'Idempotency-Key': idempotencyKey } }).then((r) => r.data),
    paymentConfig: () => http.get('/payments/config').then((r) => r.data),
    verifyRazorpay: (body) => http.post('/payments/razorpay/verify', body).then((r) => r.data),
    completeSandbox: (paymentId, success, method) => http.post(`/payments/sandbox/${paymentId}/complete`, { success, method }).then((r) => r.data),
    retry: (orderNumber, method) => http.post(`/payments/orders/${orderNumber}/retry`, { method }).then((r) => r.data),
    order: (orderNumber) => http.get(`/orders/${encodeURIComponent(orderNumber)}`).then((r) => r.data),
};
