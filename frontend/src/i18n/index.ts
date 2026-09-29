import { createI18n } from 'vue-i18n'
import en from './locales/en'

export type MessageSchema = typeof en

export const SUPPORTED_LOCALES = [{ code: 'en', label: 'English' }] as const

export type LocaleCode = (typeof SUPPORTED_LOCALES)[number]['code']

const LOCALE_KEY = 'zovira.locale'

function initialLocale(): LocaleCode {
  try {
    const saved = localStorage.getItem(LOCALE_KEY)
    if (saved && SUPPORTED_LOCALES.some((l) => l.code === saved)) return saved as LocaleCode
  } catch {
    // storage unavailable (private mode); fall through to default
  }
  return 'en'
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages: { en },
})

export function setLocale(code: LocaleCode) {
  i18n.global.locale.value = code
  document.documentElement.lang = code
  try {
    localStorage.setItem(LOCALE_KEY, code)
  } catch {
    // ignore persistence failures
  }
}
