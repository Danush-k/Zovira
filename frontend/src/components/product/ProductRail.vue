<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRight, ChevronLeft, ChevronRight } from '@lucide/vue'
import ProductCard from './ProductCard.vue'
import ProductCardSkeleton from './ProductCardSkeleton.vue'
import type { ProductSummary } from '@/types/catalog'

withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    products: ProductSummary[] | null
    loading?: boolean
    viewAllTo?: string
  }>(),
  { loading: false },
)

const track = ref<HTMLElement | null>(null)
const canPrev = ref(false)
const canNext = ref(false)

function update() {
  const el = track.value
  if (!el) return
  canPrev.value = el.scrollLeft > 4
  canNext.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 4
}

function scroll(direction: 1 | -1) {
  const el = track.value
  if (!el) return
  el.scrollBy({ left: direction * el.clientWidth * 0.85, behavior: 'smooth' })
}

let observer: ResizeObserver | undefined
onMounted(() => {
  update()
  observer = new ResizeObserver(update)
  if (track.value) observer.observe(track.value)
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
  <section class="relative">
    <header class="mb-4 flex items-end justify-between gap-4">
      <div>
        <h2 class="section-title">{{ title }}</h2>
        <p v-if="subtitle" class="mt-0.5 text-sm text-ink-500">{{ subtitle }}</p>
      </div>
      <div class="flex items-center gap-2">
        <RouterLink
          v-if="viewAllTo"
          :to="viewAllTo"
          class="group inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800"
        >
          {{ $t('common.viewAll') }}
          <ArrowRight class="size-4 transition-transform group-hover:translate-x-0.5" />
        </RouterLink>
        <div class="hidden gap-1.5 md:flex">
          <button
            type="button"
            class="grid size-8 place-items-center rounded-full border border-ink-200 bg-white text-ink-700 transition hover:border-ink-300 disabled:opacity-35"
            :disabled="!canPrev"
            :aria-label="`Scroll ${title} back`"
            @click="scroll(-1)"
          >
            <ChevronLeft class="size-4" />
          </button>
          <button
            type="button"
            class="grid size-8 place-items-center rounded-full border border-ink-200 bg-white text-ink-700 transition hover:border-ink-300 disabled:opacity-35"
            :disabled="!canNext"
            :aria-label="`Scroll ${title} forward`"
            @click="scroll(1)"
          >
            <ChevronRight class="size-4" />
          </button>
        </div>
      </div>
    </header>

    <div
      ref="track"
      class="scrollbar-none -mx-4 flex snap-x snap-mandatory gap-3 overflow-x-auto scroll-smooth px-4 pb-1 sm:-mx-6 sm:gap-4 sm:px-6 lg:mx-0 lg:px-0"
      @scroll.passive="update"
    >
      <template v-if="loading && !products">
        <div v-for="i in 6" :key="i" class="w-[46%] shrink-0 snap-start sm:w-[31%] md:w-[23%] lg:w-[calc((100%-5*1rem)/6)]">
          <ProductCardSkeleton />
        </div>
      </template>
      <div
        v-for="product in products ?? []"
        v-else
        :key="product.id"
        class="w-[46%] shrink-0 snap-start sm:w-[31%] md:w-[23%] lg:w-[calc((100%-5*1rem)/6)]"
      >
        <ProductCard :product="product">
          <template #action><slot name="card-action" :product="product" /></template>
        </ProductCard>
      </div>
    </div>
  </section>
</template>
