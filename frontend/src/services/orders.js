import { http } from './http';
export const RETURN_REASONS = [
    { value: 'DAMAGED', label: 'Arrived damaged' },
    { value: 'DEFECTIVE', label: "Defective or doesn't work" },
    { value: 'WRONG_ITEM', label: 'Received the wrong item' },
    { value: 'NOT_AS_DESCRIBED', label: 'Not as described' },
    { value: 'SIZE_FIT', label: "Size or fit isn't right" },
    { value: 'CHANGED_MIND', label: 'No longer needed' },
    { value: 'OTHER', label: 'Other' },
];
export const ordersApi = {
    list: (filter, page = 0) => http.get('/orders', { params: { filter, page, size: 10 } }).then((r) => r.data),
    detail: (orderNumber) => http.get(`/orders/${encodeURIComponent(orderNumber)}`).then((r) => r.data),
    cancel: (orderNumber, reason) => http.post(`/orders/${encodeURIComponent(orderNumber)}/cancel`, { reason }).then((r) => r.data),
    reorder: (orderNumber) => http.post(`/orders/${encodeURIComponent(orderNumber)}/reorder`).then((r) => r.data),
    async downloadInvoice(orderNumber) {
        const res = await http.get(`/orders/${encodeURIComponent(orderNumber)}/invoice`, { responseType: 'blob' });
        const url = URL.createObjectURL(res.data);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Zovira-invoice-${orderNumber}.pdf`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    },
    requestReturn: (orderNumber, itemId, body) => http.post(`/orders/${encodeURIComponent(orderNumber)}/items/${itemId}/return`, body).then((r) => r.data),
    returns: (page = 0) => http.get('/returns', { params: { page } }).then((r) => r.data),
};
