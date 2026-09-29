<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Expand } from '@lucide/vue'
import ZDialog from '@/components/ui/ZDialog.vue'

export interface GalleryImage {
  id: number | string
  url: string
  altText?: string | null
}

const props = defineProps<{ images: GalleryImage[]; title: string }>()

const active = ref(0)
const zoom = ref(false)
const origin = ref('50% 50%')
const lightbox = ref(false)
const scroller = ref<HTMLElement | null>(null)

watch(
  () => props.images.map((i) => i.url).join('|'),
  () => (active.value = 0),
)

const current = computed(() => props.images[active.value])

function onMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  origin.value = `${((e.clientX - rect.left) / rect.width) * 100}% ${((e.clientY - rect.top) / rect.height) * 100}%`
}

function go(i: number) {
  active.value = (i + props.images.length) % props.images.length
}

/** Mobile: swipeable, snap-scrolling strip keeps the active dot in sync. */
function onMobileScroll() {
  const el = scroller.value
  if (!el) return
  active.value = Math.round(el.scrollLeft / el.clientWidth)
}

function onLightboxKey(e: KeyboardEvent) {
  if (e.key === 'ArrowRight') go(active.value + 1)
  if (e.key === 'ArrowLeft') go(active.value - 1)
}
</script>

<template>
  <div>
    <!-- Desktop: thumbnails + zoomable stage -->
    <div class="hidden gap-4 md:grid md:grid-cols-[72px_minmax(0,1fr)]">
      <ul class="flex max-h-[560px] flex-col gap-2.5 overflow-y-auto" aria-label="Product images">
        <li v-for="(image, i) in images" :key="image.id">
          <button
            type="button"
            :aria-label="`Show image ${i + 1}`"
            :aria-current="i === active ? 'true' : undefined"
            :class="[
              'block aspect-square w-full overflow-hidden rounded-lg border-2 bg-ink-50 transition-colors',
              i === active ? 'border-brand-600' : 'border-transparent hover:border-ink-200',
            ]"
            @mouseenter="active = i"
            @focus="active = i"
          >
            <img :src="image.url" alt="" class="size-full object-contain p-1.5 mix-blend-multiply" />
          </button>
        </li>
      </ul>

      <div
        class="group relative aspect-square cursor-zoom-in overflow-hidden rounded-2xl border border-ink-150 bg-ink-50"
        @mouseenter="zoom = true"
        @mouseleave="zoom = false"
        @mousemove="onMove"
        @click="lightbox = true"
      >
        <img
          v-if="current"
          :src="current.url"
          :alt="current.altText || title"
          class="size-full object-contain p-8 mix-blend-multiply transition-transform duration-200 ease-out"
          :style="{ transform: zoom ? 'scale(2.1)' : 'scale(1)', transformOrigin: origin }"
          fetchpriority="high"
        />
        <button
          type="button"
          class="absolute right-3 bottom-3 inline-flex items-center gap-1.5 rounded-lg bg-white/90 px-2.5 py-1.5 text-xs font-semibold text-ink-700 opacity-0 shadow-card backdrop-blur transition-opacity group-hover:opacity-100 focus:opacity-100"
          @click.stop="lightbox = true"
        >
          <Expand class="size-3.5" />
          Full screen
        </button>
        <slot name="overlay" />
      </div>
    </div>

    <!-- Mobile: swipe gallery -->
    <div class="md:hidden">
      <div class="relative">
        <div
          ref="scroller"
          class="scrollbar-none flex snap-x snap-mandatory overflow-x-auto rounded-2xl bg-ink-50"
          @scroll.passive="onMobileScroll"
        >
          <img
            v-for="image in images"
            :key="image.id"
            :src="image.url"
            :alt="image.altText || title"
            class="aspect-square w-full shrink-0 snap-center object-contain p-6 mix-blend-multiply"
            @click="lightbox = true"
          />
        </div>
        <slot name="overlay" />
      </div>
      <div v-if="images.length > 1" class="mt-3 flex justify-center gap-1.5">
        <span
          v-for="(image, i) in images"
          :key="image.id"
          :class="['h-1.5 rounded-full transition-all', i === active ? 'w-5 bg-ink-800' : 'w-1.5 bg-ink-300']"
        />
      </div>
    </div>

    <ZDialog v-model:open="lightbox" :title="title" size="xl">
      <div class="relative" tabindex="0" @keydown="onLightboxKey">
        <img
          v-if="current"
          :src="current.url"
          :alt="current.altText || title"
          class="mx-auto max-h-[70vh] w-full object-contain"
        />
        <template v-if="images.length > 1">
          <button
            type="button"
            class="absolute top-1/2 left-0 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-white shadow-raised hover:bg-ink-50"
            aria-label="Previous image"
            @click="go(active - 1)"
          >
            <ChevronLeft class="size-5" />
          </button>
          <button
            type="button"
            class="absolute top-1/2 right-0 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-white shadow-raised hover:bg-ink-50"
            aria-label="Next image"
            @click="go(active + 1)"
          >
            <ChevronRight class="size-5" />
          </button>
        </template>
      </div>
      <div class="mt-4 flex justify-center gap-2 overflow-x-auto">
        <button
          v-for="(image, i) in images"
          :key="image.id"
          type="button"
          :class="['size-16 shrink-0 overflow-hidden rounded-lg border-2 bg-ink-50', i === active ? 'border-brand-600' : 'border-transparent']"
          :aria-label="`Show image ${i + 1}`"
          @click="active = i"
        >
          <img :src="image.url" alt="" class="size-full object-contain p-1 mix-blend-multiply" />
        </button>
      </div>
    </ZDialog>
  </div>
</template>
