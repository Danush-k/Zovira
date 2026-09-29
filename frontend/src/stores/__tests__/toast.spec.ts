import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from '../toast'

describe('toast store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  it('auto-dismisses toasts after their duration', () => {
    const store = useToastStore()
    store.success('Added to cart')
    expect(store.toasts).toHaveLength(1)
    vi.advanceTimersByTime(4000)
    expect(store.toasts).toHaveLength(0)
  })

  it('keeps at most four toasts on screen', () => {
    const store = useToastStore()
    for (let i = 0; i < 6; i++) store.info(`Toast ${i}`)
    expect(store.toasts).toHaveLength(4)
    expect(store.toasts[0]!.title).toBe('Toast 2')
  })
})
