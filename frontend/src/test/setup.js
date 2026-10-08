import { afterEach, vi } from 'vitest';
/**
 * Node 25+ ships a built-in Web Storage global that shadows jsdom's implementation and is
 * non-functional without --localstorage-file. Install a spec-compatible in-memory Storage so
 * tests behave the same on every Node version.
 */
class MemoryStorage {
    store = new Map();
    get length() {
        return this.store.size;
    }
    clear() {
        this.store.clear();
    }
    getItem(key) {
        return this.store.has(key) ? this.store.get(key) : null;
    }
    key(index) {
        return Array.from(this.store.keys())[index] ?? null;
    }
    removeItem(key) {
        this.store.delete(key);
    }
    setItem(key, value) {
        this.store.set(key, String(value));
    }
}
for (const name of ['localStorage', 'sessionStorage']) {
    if (typeof globalThis[name]?.clear !== 'function') {
        Object.defineProperty(globalThis, name, { value: new MemoryStorage(), configurable: true });
    }
}
afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
});
