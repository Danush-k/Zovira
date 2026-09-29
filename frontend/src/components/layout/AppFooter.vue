<script setup lang="ts">
import { BadgeCheck, RotateCcw, ShieldCheck, Truck } from '@lucide/vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'

const year = new Date().getFullYear()

const assurances = [
  { icon: Truck, title: 'Fast, tracked delivery', body: 'Free on orders above ₹499' },
  { icon: RotateCcw, title: 'Easy returns', body: 'Hassle-free 7-day returns' },
  { icon: ShieldCheck, title: 'Secure payments', body: 'UPI, cards, net banking, COD' },
  { icon: BadgeCheck, title: 'Genuine products', body: 'From verified sellers only' },
]
</script>

<template>
  <footer class="mt-16 border-t border-ink-150 bg-white pb-20 lg:pb-0">
    <div class="container-page grid grid-cols-2 gap-6 border-b border-ink-100 py-8 lg:grid-cols-4">
      <div v-for="item in assurances" :key="item.title" class="flex items-start gap-3">
        <span class="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
          <component :is="item.icon" class="size-5" stroke-width="1.75" />
        </span>
        <div>
          <p class="text-sm font-semibold text-ink-900">{{ item.title }}</p>
          <p class="text-[13px] text-ink-500">{{ item.body }}</p>
        </div>
      </div>
    </div>

    <div class="container-page grid gap-10 py-10 md:grid-cols-[1.4fr_repeat(3,1fr)]">
      <div class="max-w-xs">
        <BrandLogo />
        <p class="mt-4 text-sm leading-relaxed text-ink-500">{{ $t('footer.tagline') }}</p>
      </div>
      <nav aria-labelledby="footer-service">
        <h2 id="footer-service" class="text-[13px] font-semibold tracking-wide text-ink-900 uppercase">
          {{ $t('footer.customerService') }}
        </h2>
        <ul class="mt-4 space-y-2.5 text-sm text-ink-600">
          <li><RouterLink to="/help" class="hover:text-brand-700">{{ $t('footer.helpCenter') }}</RouterLink></li>
          <li><RouterLink to="/help/shipping" class="hover:text-brand-700">{{ $t('footer.shipping') }}</RouterLink></li>
          <li><RouterLink to="/help/returns" class="hover:text-brand-700">{{ $t('footer.returns') }}</RouterLink></li>
          <li><RouterLink to="/help/payments" class="hover:text-brand-700">{{ $t('footer.payments') }}</RouterLink></li>
        </ul>
      </nav>
      <nav aria-labelledby="footer-sell">
        <h2 id="footer-sell" class="text-[13px] font-semibold tracking-wide text-ink-900 uppercase">
          {{ $t('footer.sell') }}
        </h2>
        <ul class="mt-4 space-y-2.5 text-sm text-ink-600">
          <li><RouterLink to="/sell" class="hover:text-brand-700">{{ $t('footer.becomeSeller') }}</RouterLink></li>
          <li><RouterLink to="/seller" class="hover:text-brand-700">{{ $t('footer.sellerDashboard') }}</RouterLink></li>
        </ul>
      </nav>
      <nav aria-labelledby="footer-company">
        <h2 id="footer-company" class="text-[13px] font-semibold tracking-wide text-ink-900 uppercase">
          {{ $t('footer.company') }}
        </h2>
        <ul class="mt-4 space-y-2.5 text-sm text-ink-600">
          <li><RouterLink to="/help/about" class="hover:text-brand-700">{{ $t('footer.about') }}</RouterLink></li>
          <li><RouterLink to="/help/privacy" class="hover:text-brand-700">{{ $t('footer.privacy') }}</RouterLink></li>
          <li><RouterLink to="/help/terms" class="hover:text-brand-700">{{ $t('footer.terms') }}</RouterLink></li>
        </ul>
      </nav>
    </div>

    <div class="border-t border-ink-100">
      <div class="container-page flex flex-col items-center justify-between gap-2 py-5 text-[13px] text-ink-500 sm:flex-row">
        <p>&copy; {{ year }} Zovira. {{ $t('footer.rights') }}</p>
        <p>Prices shown are inclusive of GST.</p>
      </div>
    </div>
  </footer>
</template>
