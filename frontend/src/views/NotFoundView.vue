<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Home, Search } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'

const router = useRouter()
const query = ref('')

function search() {
  const q = query.value.trim()
  if (q) void router.push({ path: '/search', query: { q } })
}
</script>

<template>
  <section class="container-page flex min-h-[60vh] flex-col items-center justify-center py-20 text-center">
    <p class="text-[5.5rem] leading-none font-[780] tracking-[-0.06em] text-brand-700 sm:text-[7rem]">
      4<span class="text-accent-500">0</span>4
    </p>
    <h1 class="mt-6 text-2xl font-semibold tracking-tight text-ink-900">{{ $t('errors.notFoundTitle') }}</h1>
    <p class="mt-2 max-w-md text-ink-500">{{ $t('errors.notFoundBody') }}</p>

    <form role="search" class="relative mt-8 w-full max-w-md" @submit.prevent="search">
      <label for="notfound-search" class="sr-only">{{ $t('common.search') }}</label>
      <Search class="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-ink-400" />
      <input
        id="notfound-search"
        v-model="query"
        type="search"
        :placeholder="$t('common.searchPlaceholder')"
        class="field-control h-12 pl-10"
      />
    </form>

    <ZButton to="/" variant="outline" class="mt-4">
      <Home class="size-4" />
      {{ $t('common.goHome') }}
    </ZButton>
  </section>
</template>
