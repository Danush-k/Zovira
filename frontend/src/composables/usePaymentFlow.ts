import { ref } from 'vue'
import { checkoutApi } from '@/services/checkout'
import type { PaymentIntent, PaymentMethod, PaymentResult } from '@/types/order'

interface RazorpayResponse {
  razorpay_order_id: string
  razorpay_payment_id: string
  razorpay_signature: string
}

interface RazorpayInstance {
  open(): void
  on(event: string, handler: (response: { error?: { description?: string } }) => void): void
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => RazorpayInstance
  }
}

let scriptPromise: Promise<void> | null = null

function loadRazorpay(): Promise<void> {
  if (window.Razorpay) return Promise.resolve()
  scriptPromise ??= new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = 'https://checkout.razorpay.com/v1/checkout.js'
    s.async = true
    s.onload = () => resolve()
    s.onerror = () => {
      scriptPromise = null
      reject(new Error('Could not load the payment window. Check your connection and try again.'))
    }
    document.head.appendChild(s)
  })
  return scriptPromise
}

export class PaymentDismissedError extends Error {
  constructor() {
    super('Payment was not completed')
  }
}

/**
 * Runs the browser side of a payment. Razorpay opens its hosted checkout and the result is
 * verified by the API; the sandbox provider shows Zovira's labelled test-payment dialog instead.
 */
export function usePaymentFlow() {
  const sandboxIntent = ref<PaymentIntent | null>(null)
  const sandboxMethod = ref<PaymentMethod>('UPI')
  let settle: { resolve: (r: PaymentResult) => void; reject: (e: unknown) => void } | null = null

  async function pay(intent: PaymentIntent, method: PaymentMethod): Promise<PaymentResult> {
    if (intent.provider === 'SANDBOX') {
      sandboxIntent.value = intent
      sandboxMethod.value = method
      return new Promise((resolve, reject) => (settle = { resolve, reject }))
    }
    await loadRazorpay()
    return new Promise((resolve, reject) => {
      const rzp = new window.Razorpay!({
        key: intent.keyId,
        amount: intent.amountInPaise,
        currency: intent.currency,
        name: 'Zovira',
        description: `Order ${intent.orderNumber}`,
        order_id: intent.providerOrderId,
        prefill: { name: intent.customerName, email: intent.customerEmail, contact: intent.customerPhone },
        theme: { color: '#155A50' },
        handler: (response: RazorpayResponse) => {
          checkoutApi
            .verifyRazorpay({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })
            .then(resolve, reject)
        },
        modal: { ondismiss: () => reject(new PaymentDismissedError()) },
      })
      rzp.on('payment.failed', () => undefined)
      rzp.open()
    })
  }

  async function completeSandbox(success: boolean) {
    const intent = sandboxIntent.value
    if (!intent || !settle) return
    try {
      const result = await checkoutApi.completeSandbox(intent.paymentId, success, sandboxMethod.value)
      if (success) settle.resolve(result)
      else settle.reject(new PaymentDismissedError())
    } catch (e) {
      settle.reject(e)
    } finally {
      sandboxIntent.value = null
      settle = null
    }
  }

  function dismissSandbox() {
    settle?.reject(new PaymentDismissedError())
    settle = null
    sandboxIntent.value = null
  }

  return { pay, sandboxIntent, sandboxMethod, completeSandbox, dismissSandbox }
}
