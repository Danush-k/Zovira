import { afterEach, vi } from 'vitest'

/**
 * Node 25+ ships a built-in Web Storage global that shadows jsdom's implementation and is
 * non-functional without --localstorage-file. Install a spec-compatible in-memory Storage so
 * tests behave the same on every Node version.
 */
class MemoryStorage implements Storage {
  private store = new Map<string, string>()

  get length() {
    return this.store.size
  }

  clear() {
    this.store.clear()
  }

  getItem(key: string) {
    return this.store.has(key) ? (this.store.get(key) as string) : null
  }

  key(index: number) {
    return Array.from(this.store.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.store.delete(key)
  }

  setItem(key: string, value: string) {
    this.store.set(key, String(value))
  }
}

for (const name of ['localStorage', 'sessionStorage'] as const) {
  if (typeof globalThis[name]?.clear !== 'function') {
    Object.defineProperty(globalThis, name, { value: new MemoryStorage(), configurable: true })
  }
}

afterEach(() => {
  vi.restoreAllMocks()
  localStorage.clear()
  sessionStorage.clear()
})
