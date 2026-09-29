<script setup lang="ts">
import { useRoute } from 'vue-router'
import { GitCompareArrows, X } from '@lucide/vue'
import { useCompareStore, COMPARE_LIMIT } from '@/stores/compare'

const compare = useCompareStore()
const route = useRoute()
</script>

<template>
  <Transition
    enter-active-class="transition duration-300 ease-[cubic-bezier(0.22,1,0.36,1)]"
    enter-from-class="translate-y-6 opacity-0"
    leave-active-class="transition duration-200"
    leave-to-class="translate-y-6 opacity-0"
  >
    <div
      v-if="compare.count > 0 && route.name !== 'compare'"
      class="fixed bottom-[calc(4.75rem+env(safe-area-inset-bottom))] left-1/2 z-40 flex -translate-x-1/2 items-center gap-3 rounded-2xl border border-ink-150 bg-white p-2 pl-3 shadow-pop lg:bottom-6"
      role="region"
      aria-label="Compare tray"
    >
      <div class="flex -space-x-2">
        <span
          v-for="item in compare.items"
          :key="item.slug"
          class="relative size-10 overflow-hidden rounded-lg border-2 border-white bg-ink-50"
        >
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" class="size-full object-contain p-0.5 mix-blend-multiply" />
        </span>
      </div>
      <span class="hidden text-sm font-medium text-ink-700 sm:inline">{{ compare.count }} of {{ COMPARE_LIMIT }}</span>
      <RouterLink
        to="/compare"
        class="inline-flex h-9 items-center gap-2 rounded-xl bg-ink-900 px-4 text-sm font-semibold text-white hover:bg-ink-800"
      >
        <GitCompareArrows class="size-4" />
        Compare
      </RouterLink>
      <button
        type="button"
        class="grid size-8 place-items-center rounded-lg text-ink-400 hover:bg-ink-100 hover:text-ink-700"
        aria-label="Clear compare list"
        @click="compare.clear()"
      >
        <X class="size-4" />
      </button>
    </div>
  </Transition>
</template>
