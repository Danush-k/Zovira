<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { BadgeCheck, BarChart3, Clock, IndianRupee, Truck, XCircle } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZTextarea from '@/components/ui/ZTextarea.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import EmailVerificationNotice from '@/components/account/EmailVerificationNotice.vue'
import { sellerApi } from '@/services/seller'
import { errorMessage, fieldErrors } from '@/services/errors'
import { useAuthStore } from '@/stores/auth'
import { INDIAN_STATES } from '@/utils/india'
import type { SellerApplication, SellerProfile } from '@/types/seller'

const auth = useAuthStore()
const existing = ref<SellerProfile | null>(null)
const checking = ref(true)
const showForm = ref(false)
const form = reactive<SellerApplication>({
  storeName: '', description: '', gstin: '', supportEmail: '', supportPhone: '',
  pickupLine1: '', pickupCity: '', pickupState: '', pickupPincode: '',
})
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const saving = ref(false)

const benefits = [
  { icon: IndianRupee, title: 'Keep more of every sale', body: 'Transparent fees, weekly settlements and GST-ready invoices.' },
  { icon: Truck, title: 'Delivery handled for you', body: 'Print a label, hand the package over, and we track it to the door.' },
  { icon: BarChart3, title: 'Know what is working', body: 'Revenue, top products and stock alerts in one dashboard.' },
  { icon: BadgeCheck, title: 'Reach ready-to-buy shoppers', body: 'Your listings appear across search, category and deal pages.' },
]

onMounted(async () => {
  if (auth.isAuthenticated) {
    existing.value = await sellerApi.application().catch(() => null)
    if (auth.user) {
      form.supportEmail = auth.user.email
      form.supportPhone = auth.user.phone ?? ''
    }
  }
  checking.value = false
})

async function submit() {
  errors.value = {}
  formError.value = null
  saving.value = true
  try {
    existing.value = await sellerApi.apply({ ...form, gstin: form.gstin || undefined, description: form.description || undefined })
    showForm.value = false
  } catch (e) {
    errors.value = fieldErrors(e)
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <section class="bg-brand-950 text-white">
      <div class="container-page grid items-center gap-10 py-16 lg:grid-cols-2 lg:py-24">
        <div>
          <p class="text-[13px] font-bold tracking-wider text-accent-300 uppercase">Sell on Zovira</p>
          <h1 class="mt-3 text-[2.25rem] leading-tight font-bold tracking-tight sm:text-[2.75rem]">
            Put your products in front of shoppers across India
          </h1>
          <p class="mt-4 max-w-lg text-brand-100/85">
            List in minutes, ship with our delivery partners, and get paid on a weekly cycle. No listing fees to start.
          </p>
          <div class="mt-8 flex flex-wrap gap-3">
            <ZButton v-if="!auth.isAuthenticated" to="/login?redirect=/sell" size="lg" variant="accent">Sign in to start</ZButton>
            <ZButton v-else-if="!existing && !showForm" size="lg" variant="accent" @click="showForm = true">Start selling</ZButton>
            <ZButton v-else-if="existing?.status === 'APPROVED'" to="/seller" size="lg" variant="accent">Go to dashboard</ZButton>
          </div>
        </div>
        <ul class="grid gap-4 sm:grid-cols-2">
          <li v-for="b in benefits" :key="b.title" class="rounded-xl bg-white/5 p-5">
            <component :is="b.icon" class="size-6 text-accent-300" stroke-width="1.75" />
            <p class="mt-3 font-semibold">{{ b.title }}</p>
            <p class="mt-1 text-sm text-brand-100/80">{{ b.body }}</p>
          </li>
        </ul>
      </div>
    </section>

    <div class="container-page max-w-2xl py-12">
      <div v-if="checking" class="skeleton h-40 rounded-xl" />

      <div v-else-if="existing" class="surface p-6 text-center">
        <template v-if="existing.status === 'PENDING'">
          <Clock class="mx-auto size-12 text-warning" stroke-width="1.5" />
          <h2 class="mt-4 text-xl font-semibold">Your application is under review</h2>
          <p class="mt-2 text-ink-600">We're checking the details for <span class="font-semibold">{{ existing.storeName }}</span>. Most applications are reviewed within two business days.</p>
        </template>
        <template v-else-if="existing.status === 'APPROVED'">
          <BadgeCheck class="mx-auto size-12 text-success" stroke-width="1.5" />
          <h2 class="mt-4 text-xl font-semibold">You're selling on Zovira</h2>
          <p class="mt-2 text-ink-600">Manage listings, orders and payouts from your dashboard.</p>
          <ZButton to="/seller" class="mt-6">Open seller dashboard</ZButton>
        </template>
        <template v-else>
          <XCircle class="mx-auto size-12 text-danger" stroke-width="1.5" />
          <h2 class="mt-4 text-xl font-semibold">{{ existing.status === 'REJECTED' ? 'Application not approved' : 'Account suspended' }}</h2>
          <p class="mt-2 text-ink-600">{{ existing.statusReason ?? 'Contact support if you think this is a mistake.' }}</p>
        </template>
      </div>

      <form v-else-if="showForm" class="surface space-y-5 p-6" novalidate @submit.prevent="submit">
        <h2 class="text-xl font-semibold">Tell us about your store</h2>
        <EmailVerificationNotice />
        <FormAlert v-if="formError" :message="formError" />
        <ZInput v-model="form.storeName" label="Store name" :error="errors.storeName" hint="How shoppers will see you" />
        <ZTextarea v-model="form.description" label="What do you sell?" :maxlength="1000" :rows="3" optional />
        <div class="grid gap-4 sm:grid-cols-2">
          <ZInput v-model="form.supportEmail" label="Support email" type="email" :error="errors.supportEmail" />
          <ZInput v-model="form.supportPhone" label="Support phone" maxlength="10" inputmode="numeric" :error="errors.supportPhone" />
        </div>
        <ZInput v-model="form.gstin" label="GSTIN" optional maxlength="15" :error="errors.gstin" hint="Required to sell taxable goods" />
        <fieldset class="space-y-4">
          <legend class="field-label">Pickup address</legend>
          <ZInput v-model="form.pickupLine1" label="Address" :error="errors.pickupLine1" />
          <div class="grid gap-4 sm:grid-cols-3">
            <ZInput v-model="form.pickupCity" label="City" :error="errors.pickupCity" />
            <ZSelect v-model="form.pickupState" label="State" placeholder="Select" :options="INDIAN_STATES.map((s) => ({ value: s, label: s }))" :error="errors.pickupState" />
            <ZInput v-model="form.pickupPincode" label="PIN code" maxlength="6" inputmode="numeric" :error="errors.pickupPincode" />
          </div>
        </fieldset>
        <div class="flex justify-end gap-3">
          <ZButton type="button" variant="ghost" @click="showForm = false">Cancel</ZButton>
          <ZButton type="submit" :loading="saving">Submit application</ZButton>
        </div>
      </form>
    </div>
  </div>
</template>
