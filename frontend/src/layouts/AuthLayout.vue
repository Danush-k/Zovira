<script setup lang="ts">
import { RotateCcw, ShieldCheck, Truck } from '@lucide/vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'

const points = [
  { icon: Truck, title: 'Tracked delivery across India', body: 'Know exactly where your order is, every step of the way.' },
  { icon: ShieldCheck, title: 'Payments you can trust', body: 'UPI, cards, net banking and cash on delivery, verified securely.' },
  { icon: RotateCcw, title: 'Returns without the runaround', body: 'Start a return in two taps and get refunded fast.' },
]
</script>

<template>
  <div class="grid min-h-dvh lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
    <aside class="relative hidden overflow-hidden bg-brand-900 p-12 text-white lg:flex lg:flex-col">
      <div
        class="pointer-events-none absolute -top-40 -right-40 size-[520px] rounded-full border border-white/5"
        aria-hidden="true"
      />
      <div
        class="pointer-events-none absolute -top-24 -right-24 size-[360px] rounded-full border border-white/5"
        aria-hidden="true"
      />
      <RouterLink to="/" class="relative w-fit" aria-label="Zovira home">
        <BrandLogo inverse />
      </RouterLink>
      <div class="relative mt-auto max-w-md">
        <h2 class="text-[2rem] leading-tight font-semibold tracking-tight">
          Everything you love, delivered with care.
        </h2>
        <ul class="mt-10 space-y-6">
          <li v-for="p in points" :key="p.title" class="flex gap-4">
            <span class="grid size-10 shrink-0 place-items-center rounded-xl bg-white/10 text-accent-300">
              <component :is="p.icon" class="size-5" stroke-width="1.75" />
            </span>
            <div>
              <p class="font-semibold">{{ p.title }}</p>
              <p class="mt-0.5 text-sm text-brand-100/80">{{ p.body }}</p>
            </div>
          </li>
        </ul>
      </div>
      <p class="relative mt-12 text-[13px] text-brand-100/60">&copy; {{ new Date().getFullYear() }} Zovira</p>
    </aside>

    <main class="flex flex-col bg-white">
      <div class="flex h-16 items-center px-6 lg:hidden">
        <RouterLink to="/" aria-label="Zovira home"><BrandLogo /></RouterLink>
      </div>
      <div class="flex flex-1 items-center justify-center px-6 py-10 sm:px-10">
        <div class="w-full max-w-[420px]">
          <RouterView />
        </div>
      </div>
    </main>
  </div>
</template>
