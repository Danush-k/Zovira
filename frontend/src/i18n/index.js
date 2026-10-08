import { createI18n } from 'vue-i18n';
import en from './locales/en';
export const SUPPORTED_LOCALES = [{ code: 'en', label: 'English' }];
const LOCALE_KEY = 'zovira.locale';
function initialLocale() {
    try {
        const saved = localStorage.getItem(LOCALE_KEY);
        if (saved && SUPPORTED_LOCALES.some((l) => l.code === saved))
            return saved;
    }
    catch {
        // storage unavailable (private mode); fall through to default
    }
    return 'en';
}
export const i18n = createI18n({
    legacy: false,
    locale: initialLocale(),
    fallbackLocale: 'en',
    messages: { en },
});
export function setLocale(code) {
    i18n.global.locale.value = code;
    document.documentElement.lang = code;
    try {
        localStorage.setItem(LOCALE_KEY, code);
    }
    catch {
        // ignore persistence failures
    }
}
