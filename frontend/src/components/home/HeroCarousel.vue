<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowRight, ChevronLeft, ChevronRight, Pause, Play } from '@lucide/vue'
import type { HeroSlide } from '@/types/catalog'

const props = defineProps<{ slides: HeroSlide[] }>()

const index = ref(0)
const paused = ref(false)
const userPaused = ref(false)
const reducedMotion = typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
let timer: ReturnType<typeof setInterval> | undefined

const tones: Record<HeroSlide['tone'], { bg: string; accent: string; ring: string }> = {
  sand: { bg: 'bg-[#f3ebdd]', accent: 'text-accent-700', ring: 'bg-[#eadcc4]' },
  teal: { bg: 'bg-brand-100/70', accent: 'text-brand-700', ring: 'bg-brand-200/60' },
  lilac: { bg: 'bg-[#ebe7f3]', accent: 'text-[#5a4a86]', ring: 'bg-[#ddd6ec]' },
  mint: { bg: 'bg-[#e4f1e6]', accent: 'text-success', ring: 'bg-[#d0e7d4]' },
}

const current = computed(() => props.slides[index.value])

function go(i: number) {
  index.value = (i + props.slides.length) % props.slides.length
}

function start() {
  stop()
  if (reducedMotion || userPaused.value || props.slides.length < 2) return
  timer = setInterval(() => {
    if (!paused.value) go(index.value + 1)
  }, 6500)
}

function stop() {
  clearInterval(timer)
}

function togglePlay() {
  userPaused.value = !userPaused.value
  if (userPaused.value) stop()
  else start()
}

watch(() => props.slides.length, start)
onMounted(start)
onBeforeUnmount(stop)
</script>

<template>
  <section
    v-if="slides.length"
    class="relative overflow-hidden rounded-2xl"
    aria-roledescription="carousel"
    aria-label="Featured promotions"
    @mouseenter="paused = true"
    @mouseleave="paused = false"
    @focusin="paused = true"
    @focusout="paused = false"
  >
    <Transition
      mode="out-in"
      enter-active-class="transition duration-500 ease-[cubic-bezier(0.22,1,0.36,1)]"
      enter-from-class="opacity-0"
      leave-active-class="transition duration-200"
      leave-to-class="opacity-0"
    >
      <div
        v-if="current"
        :key="index"
        :class="['grid min-h-[340px] items-center gap-6 px-6 py-8 sm:px-10 md:grid-cols-[1.1fr_1fr] md:py-10 lg:min-h-[400px] lg:px-14', tones[current.tone].bg]"
        role="group"
        aria-roledescription="slide"
        :aria-label="`${index + 1} of ${slides.length}`"
      >
        <div class="relative z-10 max-w-lg">
          <p :class="['text-[13px] font-bold tracking-wider uppercase', tones[current.tone].accent]">{{ current.eyebrow }}</p>
          <h2 class="mt-3 text-[1.9rem] leading-[1.1] font-bold tracking-tight text-ink-950 sm:text-[2.5rem]">
            {{ current.title }}
          </h2>
          <p class="mt-4 max-w-md text-[15px] leading-relaxed text-ink-700 sm:text-base">{{ current.subtitle }}</p>
          <RouterLink
            :to="current.ctaLink"
            class="group mt-7 inline-flex h-12 items-center gap-2 rounded-xl bg-ink-950 px-6 text-[15px] font-semibold text-white transition-colors hover:bg-ink-800"
          >
            {{ current.ctaLabel }}
            <ArrowRight class="size-4 transition-transform group-hover:translate-x-0.5" />
          </RouterLink>
        </div>
        <div class="relative mx-auto flex aspect-square w-full max-w-[340px] items-center justify-center lg:max-w-[380px]">
          <div :class="['absolute inset-4 rounded-full', tones[current.tone].ring]" aria-hidden="true" />
          <img
            :src="current.imageUrl"
            alt=""
            class="relative size-[88%] object-contain mix-blend-multiply drop-shadow-sm"
            fetchpriority="high"
          />
        </div>
      </div>
    </Transition>

    <div class="absolute right-4 bottom-4 flex items-center gap-2 sm:right-6 sm:bottom-6">
      <button
        type="button"
        class="grid size-9 place-items-center rounded-full bg-white/80 text-ink-800 backdrop-blur transition hover:bg-white"
        :aria-label="userPaused ? 'Play slideshow' : 'Pause slideshow'"
        @click="togglePlay"
      >
        <Play v-if="userPaused" class="size-4" />
        <Pause v-else class="size-4" />
      </button>
      <button
        type="button"
        class="grid size-9 place-items-center rounded-full bg-white/80 text-ink-800 backdrop-blur transition hover:bg-white"
        aria-label="Previous slide"
        @click="go(index - 1)"
      >
        <ChevronLeft class="size-4" />
      </button>
      <button
        type="button"
        class="grid size-9 place-items-center rounded-full bg-white/80 text-ink-800 backdrop-blur transition hover:bg-white"
        aria-label="Next slide"
        @click="go(index + 1)"
      >
        <ChevronRight class="size-4" />
      </button>
    </div>
    <div class="absolute bottom-5 left-6 flex gap-1.5 sm:bottom-7 sm:left-10 lg:left-14" role="tablist" aria-label="Choose slide">
      <button
        v-for="(slide, i) in slides"
        :key="slide.title"
        type="button"
        role="tab"
        :aria-selected="i === index"
        :aria-label="`Slide ${i + 1}: ${slide.title}`"
        :class="['h-1.5 rounded-full transition-all duration-300', i === index ? 'w-7 bg-ink-900' : 'w-1.5 bg-ink-900/25 hover:bg-ink-900/40']"
        @click="go(i)"
      />
    </div>
  </section>
</template>
