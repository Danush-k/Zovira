<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Building2, CreditCard, FileText, Lock, RotateCcw, Truck } from '@lucide/vue'
import ZBreadcrumbs from '@/components/ui/ZBreadcrumbs.vue'

interface Topic {
  slug: string
  title: string
  summary: string
  icon: typeof Truck
  sections: { heading: string; body: string[] }[]
}

const topics: Topic[] = [
  {
    slug: 'shipping',
    title: 'Shipping & delivery',
    summary: 'Delivery options, charges and how tracking works.',
    icon: Truck,
    sections: [
      {
        heading: 'Delivery options',
        body: [
          'Standard delivery usually arrives within 3 to 5 business days. It is free on orders of ₹499 or more; a flat fee applies to smaller orders.',
          'Express delivery arrives in 1 to 2 business days in most serviceable pincodes for a flat fee shown at checkout.',
        ],
      },
      {
        heading: 'Tracking your order',
        body: [
          'Every order is split into shipments by seller. Each shipment has its own tracking timeline, visible under Orders in your account.',
          'You will receive a notification when a shipment is dispatched, out for delivery and delivered.',
        ],
      },
    ],
  },
  {
    slug: 'returns',
    title: 'Returns & refunds',
    summary: 'Return windows, eligibility and refund timelines.',
    icon: RotateCcw,
    sections: [
      {
        heading: 'Return window',
        body: [
          'Most products can be returned within 7 days of delivery. The exact window is shown on each product page and on your order.',
          'Some items, such as personal care products, are not returnable once opened. Non-returnable items are clearly marked.',
        ],
      },
      {
        heading: 'How refunds work',
        body: [
          'Start a return from the order details page. Once the seller approves it and the item is picked up, your refund is issued.',
          'Online payments are refunded to the original payment method. Cash on delivery orders are refunded to your bank account or UPI ID.',
        ],
      },
    ],
  },
  {
    slug: 'payments',
    title: 'Payments',
    summary: 'Accepted payment methods and payment security.',
    icon: CreditCard,
    sections: [
      {
        heading: 'Accepted methods',
        body: [
          'Pay with UPI, credit or debit cards, net banking, popular wallets, or cash on delivery for eligible orders.',
          'Cash on delivery is available on orders up to ₹50,000 where the seller supports it.',
        ],
      },
      {
        heading: 'Security',
        body: [
          'Card details are entered directly with our payment partner and are never stored by Zovira.',
          'Every payment is verified on our servers with the payment provider before an order is confirmed.',
        ],
      },
    ],
  },
  {
    slug: 'about',
    title: 'About Zovira',
    summary: 'Who we are and how the marketplace works.',
    icon: Building2,
    sections: [
      {
        heading: 'A marketplace built on trust',
        body: [
          'Zovira connects shoppers with verified independent sellers across electronics, fashion, home, beauty, books and more.',
          'Every seller is reviewed before listing, and every order is covered by our delivery and returns commitments.',
        ],
      },
    ],
  },
  {
    slug: 'privacy',
    title: 'Privacy policy',
    summary: 'What we collect, why, and the controls you have.',
    icon: Lock,
    sections: [
      {
        heading: 'Information we collect',
        body: [
          'Account details you provide (name, email, phone), delivery addresses, order history, and on-site activity such as searches and recently viewed products.',
          'We use this information to fulfil orders, personalise recommendations and keep your account secure.',
        ],
      },
      {
        heading: 'Your controls',
        body: [
          'You can clear your search and browsing history, manage addresses, and sign out of other devices from your account settings at any time.',
        ],
      },
    ],
  },
  {
    slug: 'terms',
    title: 'Terms of use',
    summary: 'The rules for using Zovira as a shopper or seller.',
    icon: FileText,
    sections: [
      {
        heading: 'Using Zovira',
        body: [
          'By creating an account you agree to provide accurate information and to use the marketplace lawfully.',
          'Sellers are responsible for the accuracy of their listings, pricing and fulfilment. Zovira may suspend listings or accounts that violate marketplace policies.',
        ],
      },
    ],
  },
]

const route = useRoute()
const active = computed(() => topics.find((t) => t.slug === route.params.topic))
</script>

<template>
  <div class="container-page py-8">
    <ZBreadcrumbs
      :items="active ? [{ label: 'Home', to: '/' }, { label: 'Help center', to: '/help' }, { label: active.title }] : [{ label: 'Home', to: '/' }, { label: 'Help center' }]"
    />

    <template v-if="!active">
      <h1 class="mt-6 text-2xl font-semibold tracking-tight sm:text-3xl">How can we help?</h1>
      <p class="mt-2 text-ink-500">Answers to common questions about orders, delivery, returns and payments.</p>
      <ul class="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <li v-for="t in topics" :key="t.slug">
          <RouterLink
            :to="`/help/${t.slug}`"
            class="surface flex h-full gap-4 p-5 transition-shadow hover:shadow-raised"
          >
            <span class="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
              <component :is="t.icon" class="size-5" stroke-width="1.75" />
            </span>
            <span>
              <span class="block font-semibold text-ink-900">{{ t.title }}</span>
              <span class="mt-1 block text-sm text-ink-500">{{ t.summary }}</span>
            </span>
          </RouterLink>
        </li>
      </ul>
    </template>

    <article v-else class="mt-6 max-w-3xl">
      <h1 class="text-2xl font-semibold tracking-tight sm:text-3xl">{{ active.title }}</h1>
      <p class="mt-2 text-ink-500">{{ active.summary }}</p>
      <section v-for="s in active.sections" :key="s.heading" class="mt-8">
        <h2 class="text-lg font-semibold">{{ s.heading }}</h2>
        <p v-for="(para, i) in s.body" :key="i" class="mt-3 leading-relaxed text-ink-700">{{ para }}</p>
      </section>
    </article>
  </div>
</template>
