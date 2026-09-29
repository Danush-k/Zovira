<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown, Zap } from '@lucide/vue'
import CategoryIcon from '@/components/catalog/CategoryIcon.vue'
import { useCatalogStore } from '@/stores/catalog'
import type { CategoryNode } from '@/types/catalog'

const catalog = useCatalogStore()
const openId = ref<number | null>(null)
let timer: ReturnType<typeof setTimeout> | undefined

function enter(category: CategoryNode) {
  clearTimeout(timer)
  timer = setTimeout(() => (openId.value = category.children.length ? category.id : null), 90)
}

function leave() {
  clearTimeout(timer)
  timer = setTimeout(() => (openId.value = null), 120)
}
</script>

<template>
  <nav class="hidden border-b border-ink-150 bg-white lg:block" aria-label="Categories">
    <div class="container-page flex h-11 items-center gap-1">
      <ul class="flex min-w-0 flex-1 items-center gap-0">
        <li
          v-for="category in catalog.categories"
          :key="category.id"
          class="relative"
          @mouseenter="enter(category)"
          @mouseleave="leave"
          @keydown.esc="openId = null"
        >
          <RouterLink
            :to="`/c/${category.slug}`"
            class="flex items-center gap-1.5 rounded-md px-2 py-1.5 text-[13px] font-medium whitespace-nowrap text-ink-700 hover:bg-ink-50 hover:text-ink-900"
            :aria-expanded="category.children.length ? openId === category.id : undefined"
            @focus="enter(category)"
          >
            <CategoryIcon :name="category.icon" class="hidden size-4 text-ink-500 2xl:block" />
            {{ category.name }}
            <ChevronDown v-if="category.children.length" class="size-3 text-ink-400" />
          </RouterLink>

          <Transition
            enter-active-class="transition duration-150 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            leave-active-class="transition duration-100"
            leave-to-class="opacity-0"
          >
            <div
              v-if="openId === category.id"
              class="absolute top-full left-0 z-50 mt-1 w-[480px] rounded-xl border border-ink-150 bg-white p-4 shadow-pop"
              @mouseenter="enter(category)"
              @mouseleave="leave"
            >
              <div class="grid grid-cols-[1fr_140px] gap-4">
                <div>
                  <p class="px-2 text-xs font-semibold tracking-wide text-ink-400 uppercase">{{ category.name }}</p>
                  <ul class="mt-2 grid gap-0.5">
                    <li v-for="child in category.children" :key="child.id">
                      <RouterLink
                        :to="`/c/${child.slug}`"
                        class="block rounded-lg px-2 py-2 text-sm font-medium text-ink-800 hover:bg-ink-50 hover:text-brand-800"
                        @click="openId = null"
                      >
                        {{ child.name }}
                      </RouterLink>
                    </li>
                  </ul>
                  <RouterLink
                    :to="`/c/${category.slug}`"
                    class="mt-2 block px-2 text-[13px] font-semibold text-brand-700 hover:underline"
                    @click="openId = null"
                  >
                    Shop all {{ category.name }}
                  </RouterLink>
                </div>
                <RouterLink
                  :to="`/c/${category.slug}`"
                  class="block overflow-hidden rounded-lg bg-ink-50"
                  tabindex="-1"
                  @click="openId = null"
                >
                  <img
                    v-if="category.imageUrl"
                    :src="category.imageUrl"
                    alt=""
                    class="aspect-square w-full object-contain p-3 mix-blend-multiply"
                  />
                </RouterLink>
              </div>
            </div>
          </Transition>
        </li>
      </ul>
      <RouterLink
        to="/search?sort=discount"
        class="flex shrink-0 items-center gap-1.5 rounded-md px-2.5 py-1.5 text-[13.5px] font-semibold text-accent-700 hover:bg-accent-50"
      >
        <Zap class="size-4" />
        {{ $t('nav.deals') }}
      </RouterLink>
    </div>
  </nav>
</template>
