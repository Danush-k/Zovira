<script setup lang="ts">
import { ref } from 'vue'
import { ChevronRight } from '@lucide/vue'
import ZDrawer from '@/components/ui/ZDrawer.vue'
import CategoryIcon from '@/components/catalog/CategoryIcon.vue'
import { useCatalogStore } from '@/stores/catalog'

const open = defineModel<boolean>('open', { default: false })
const catalog = useCatalogStore()
const expanded = ref<number | null>(null)
</script>

<template>
  <ZDrawer v-model:open="open" side="left" width="max-w-sm" :title="$t('nav.allCategories')">
    <ul class="p-2">
      <li v-for="category in catalog.categories" :key="category.id" class="border-b border-ink-100 last:border-0">
        <div class="flex items-center">
          <RouterLink
            :to="`/c/${category.slug}`"
            class="flex flex-1 items-center gap-3 px-3 py-3.5 text-[15px] font-medium text-ink-800"
            @click="open = false"
          >
            <CategoryIcon :name="category.icon" class="size-5 text-ink-500" />
            {{ category.name }}
          </RouterLink>
          <button
            v-if="category.children.length"
            type="button"
            class="grid size-11 place-items-center text-ink-500"
            :aria-expanded="expanded === category.id"
            :aria-label="`Show ${category.name} subcategories`"
            @click="expanded = expanded === category.id ? null : category.id"
          >
            <ChevronRight :class="['size-4 transition-transform', expanded === category.id && 'rotate-90']" />
          </button>
        </div>
        <ul v-if="expanded === category.id" class="mb-2 ml-11 space-y-0.5">
          <li v-for="child in category.children" :key="child.id">
            <RouterLink
              :to="`/c/${child.slug}`"
              class="block rounded-lg px-3 py-2 text-sm text-ink-600 hover:bg-ink-50"
              @click="open = false"
            >
              {{ child.name }}
            </RouterLink>
          </li>
        </ul>
      </li>
    </ul>
  </ZDrawer>
</template>
