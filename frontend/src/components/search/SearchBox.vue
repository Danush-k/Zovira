<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Search, Tag, TrendingUp, X } from '@lucide/vue'
import { searchApi } from '@/services/search'
import { useAuthStore } from '@/stores/auth'
import { useClickOutside } from '@/composables/useClickOutside'
import { formatPrice } from '@/utils/format'
import type { Suggestions } from '@/types/search'

const props = withDefaults(defineProps<{ inputId: string; variant?: 'desktop' | 'mobile' }>(), { variant: 'desktop' })

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const root = ref<HTMLElement | null>(null)
const input = ref<HTMLInputElement | null>(null)
const query = ref(typeof route.query.q === 'string' ? route.query.q : '')
const open = ref(false)
const active = ref(-1)
const suggestions = ref<Suggestions | null>(null)
const recent = ref<string[]>([])
const trending = ref<string[]>([])
let controller: AbortController | null = null
let timer: ReturnType<typeof setTimeout> | undefined

useClickOutside([root], () => (open.value = false))

watch(
  () => route.query.q,
  (q) => (query.value = typeof q === 'string' ? q : ''),
)

watch(query, (q) => {
  clearTimeout(timer)
  active.value = -1
  if (q.trim().length < 2) {
    suggestions.value = null
    return
  }
  timer = setTimeout(async () => {
    controller?.abort()
    controller = new AbortController()
    try {
      suggestions.value = await searchApi.suggest(q.trim(), controller.signal)
    } catch {
      // aborted or failed; keep previous suggestions
    }
  }, 180)
})

async function onFocus() {
  open.value = true
  if (!trending.value.length) trending.value = await searchApi.trending().catch(() => [])
  if (auth.isAuthenticated) recent.value = await searchApi.history().catch(() => [])
}

type Item = { kind: 'query' | 'product' | 'category' | 'brand'; label: string; to: string; image?: string | null; price?: number }

const items = computed<Item[]>(() => {
  const s = suggestions.value
  if (query.value.trim().length >= 2 && s) {
    return [
      ...s.queries.map((q) => ({ kind: 'query' as const, label: q, to: `/search?q=${encodeURIComponent(q)}` })),
      ...s.categories.map((c) => ({ kind: 'category' as const, label: c.name, to: `/c/${c.slug}` })),
      ...s.brands.map((b) => ({ kind: 'brand' as const, label: b.name, to: `/brand/${b.slug}` })),
      ...s.products.map((p) => ({ kind: 'product' as const, label: p.title, to: `/p/${p.slug}`, image: p.imageUrl, price: p.price })),
    ]
  }
  const history = recent.value.map((q) => ({ kind: 'query' as const, label: q, to: `/search?q=${encodeURIComponent(q)}` }))
  return history.length ? history : trending.value.map((q) => ({ kind: 'query' as const, label: q, to: `/search?q=${encodeURIComponent(q)}` }))
})

const emptyHeading = computed(() => (recent.value.length ? 'Recent searches' : 'Popular right now'))

function submit() {
  if (active.value >= 0 && items.value[active.value]) return go(items.value[active.value]!.to)
  const q = query.value.trim()
  if (q) go(`/search?q=${encodeURIComponent(q)}`)
}

function go(to: string) {
  open.value = false
  input.value?.blur()
  void router.push(to)
}

function onKeydown(e: KeyboardEvent) {
  if (!open.value || !items.value.length) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    active.value = (active.value + 1) % items.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    active.value = active.value <= 0 ? items.value.length - 1 : active.value - 1
  } else if (e.key === 'Escape') {
    open.value = false
  }
}

async function clearHistory() {
  await searchApi.clearHistory().catch(() => undefined)
  recent.value = []
}

function focus() {
  input.value?.focus()
}

defineExpose({ focus })
</script>

<template>
  <div ref="root" class="relative">
    <form role="search" @submit.prevent="submit">
      <label :for="inputId" class="sr-only">{{ $t('common.search') }}</label>
      <Search
        v-if="props.variant === 'mobile'"
        class="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-ink-400"
      />
      <input
        :id="inputId"
        ref="input"
        v-model="query"
        type="search"
        autocomplete="off"
        enterkeyhint="search"
        role="combobox"
        :aria-expanded="open && items.length > 0"
        :aria-controls="`${inputId}-listbox`"
        :aria-activedescendant="active >= 0 ? `${inputId}-opt-${active}` : undefined"
        :placeholder="$t('common.searchPlaceholder')"
        :class="[
          'h-11 w-full rounded-xl border border-ink-200 bg-ink-50 text-[15px] placeholder:text-ink-400 hover:border-ink-300 focus:border-brand-500 focus:bg-white focus:ring-3 focus:ring-brand-500/15 focus:outline-none',
          props.variant === 'mobile' ? 'pr-4 pl-10' : 'pr-12 pl-4',
        ]"
        @focus="onFocus"
        @keydown="onKeydown"
      />
      <button
        v-if="props.variant === 'desktop'"
        type="submit"
        class="absolute top-1 right-1 grid size-9 place-items-center rounded-lg bg-brand-700 text-white hover:bg-brand-800"
        :aria-label="$t('common.search')"
      >
        <Search class="size-[18px]" />
      </button>
    </form>

    <div
      v-if="open && items.length"
      :id="`${inputId}-listbox`"
      role="listbox"
      class="absolute inset-x-0 top-full z-50 mt-2 overflow-hidden rounded-xl border border-ink-150 bg-white py-2 shadow-pop"
    >
      <div v-if="query.trim().length < 2" class="flex items-center justify-between px-4 pt-1 pb-2">
        <p class="text-xs font-semibold tracking-wide text-ink-400 uppercase">{{ emptyHeading }}</p>
        <button v-if="recent.length" type="button" class="text-xs font-semibold text-ink-500 hover:text-ink-800" @click="clearHistory">
          Clear
        </button>
      </div>
      <button
        v-for="(item, i) in items"
        :id="`${inputId}-opt-${i}`"
        :key="`${item.kind}-${item.to}`"
        type="button"
        role="option"
        :aria-selected="i === active"
        :class="['flex w-full items-center gap-3 px-4 py-2 text-left text-sm', i === active ? 'bg-ink-50' : 'hover:bg-ink-50']"
        @mouseenter="active = i"
        @click="go(item.to)"
      >
        <span v-if="item.kind === 'product'" class="grid size-9 shrink-0 place-items-center overflow-hidden rounded-md bg-ink-50">
          <img v-if="item.image" :src="item.image" alt="" class="size-full object-contain p-0.5 mix-blend-multiply" />
        </span>
        <Clock v-else-if="item.kind === 'query' && recent.length && query.trim().length < 2" class="size-4 shrink-0 text-ink-400" />
        <TrendingUp v-else-if="item.kind === 'query'" class="size-4 shrink-0 text-ink-400" />
        <Tag v-else class="size-4 shrink-0 text-ink-400" />
        <span class="min-w-0 flex-1 truncate text-ink-800">{{ item.label }}</span>
        <span v-if="item.kind === 'category'" class="text-xs text-ink-400">Category</span>
        <span v-else-if="item.kind === 'brand'" class="text-xs text-ink-400">Brand</span>
        <span v-else-if="item.price" class="tabular text-xs font-semibold text-ink-600">{{ formatPrice(item.price) }}</span>
      </button>
    </div>
    <button
      v-if="open && query && props.variant === 'mobile'"
      type="button"
      class="absolute top-1/2 right-2 grid size-8 -translate-y-1/2 place-items-center text-ink-400"
      aria-label="Clear search"
      @click="query = ''"
    >
      <X class="size-4" />
    </button>
  </div>
</template>
