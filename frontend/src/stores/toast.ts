import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastTone = 'success' | 'error' | 'info' | 'warning'

export interface ToastAction {
  label: string
  to?: string
  handler?: () => void
}

export interface Toast {
  id: number
  tone: ToastTone
  title: string
  message?: string
  action?: ToastAction
  duration: number
}

let nextId = 1

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  const timers = new Map<number, ReturnType<typeof setTimeout>>()

  function dismiss(id: number) {
    toasts.value = toasts.value.filter((t) => t.id !== id)
    clearTimeout(timers.get(id))
    timers.delete(id)
  }

  function push(toast: Omit<Toast, 'id' | 'duration'> & { duration?: number }) {
    const id = nextId++
    const duration = toast.duration ?? (toast.tone === 'error' ? 6000 : 4000)
    toasts.value = [...toasts.value.slice(-3), { ...toast, id, duration }]
    if (duration > 0) timers.set(id, setTimeout(() => dismiss(id), duration))
    return id
  }

  const success = (title: string, message?: string, action?: ToastAction) =>
    push({ tone: 'success', title, message, action })
  const error = (title: string, message?: string) => push({ tone: 'error', title, message })
  const info = (title: string, message?: string, action?: ToastAction) => push({ tone: 'info', title, message, action })
  const warning = (title: string, message?: string) => push({ tone: 'warning', title, message })

  return { toasts, push, dismiss, success, error, info, warning }
})
