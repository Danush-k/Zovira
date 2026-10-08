import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { catalogApi } from '@/services/catalog';
/** Long-lived storefront reference data: the category tree and public platform settings. */
export const useCatalogStore = defineStore('catalog', () => {
    const categories = ref([]);
    const settings = ref(null);
    let categoriesPromise = null;
    let settingsPromise = null;
    function loadCategories() {
        if (!categoriesPromise) {
            categoriesPromise = catalogApi
                .categories()
                .then((tree) => (categories.value = tree))
                .catch((e) => {
                categoriesPromise = null;
                throw e;
            });
        }
        return categoriesPromise;
    }
    function loadSettings() {
        if (!settingsPromise) {
            settingsPromise = catalogApi
                .settings()
                .then((s) => (settings.value = s))
                .catch(() => {
                settingsPromise = null;
                return null;
            });
        }
        return settingsPromise;
    }
    const flat = computed(() => {
        const out = [];
        const walk = (nodes, parent) => nodes.forEach((n) => {
            out.push({ ...n, parent });
            walk(n.children, n);
        });
        walk(categories.value, null);
        return out;
    });
    function findBySlug(slug) {
        return flat.value.find((c) => c.slug === slug) ?? null;
    }
    return { categories, settings, loadCategories, loadSettings, flat, findBySlug };
});
