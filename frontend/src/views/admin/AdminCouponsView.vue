<script setup>
import { reactive, ref } from "vue";
import { adminApi } from "@/services/admin";
import { errorMessage, fieldErrors } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useToastStore } from "@/stores/toast";
const { data, loading, error, execute } = useAsync(() => adminApi.coupons(), { immediate: true });
const toast = useToastStore();
const open = ref(false);
const editing = ref(null);
const errors = ref({});
const formError = ref(null);
const saving = ref(false);
const busy = ref(null);
const form = reactive({
  code: "",
  description: "",
  discountType: "PERCENTAGE",
  discountValue: 10,
  minOrderValue: 0,
  maxDiscount: null,
  expiresAt: null,
  usageLimit: null,
  perUserLimit: 1,
  scope: "ALL",
  active: true
});
function add() {
  editing.value = null;
  Object.assign(form, { code: "", description: "", discountType: "PERCENTAGE", discountValue: 10, minOrderValue: 0, maxDiscount: null, expiresAt: null, usageLimit: null, perUserLimit: 1, scope: "ALL", active: true });
  errors.value = {};
  formError.value = null;
  open.value = true;
}
function edit(c) {
  editing.value = c;
  Object.assign(form, {
    code: c.code,
    description: c.description,
    discountType: c.discountType,
    discountValue: c.discountValue,
    minOrderValue: c.minOrderValue,
    maxDiscount: c.maxDiscount,
    expiresAt: c.expiresAt ? c.expiresAt.slice(0, 10) : null,
    usageLimit: c.usageLimit,
    perUserLimit: c.perUserLimit,
    scope: c.scope,
    active: c.active
  });
  errors.value = {};
  formError.value = null;
  open.value = true;
}
async function save() {
  errors.value = {};
  formError.value = null;
  saving.value = true;
  try {
    const payload = { ...form, expiresAt: form.expiresAt ? (/* @__PURE__ */ new Date(`${form.expiresAt}T23:59:59`)).toISOString() : null };
    if (editing.value) await adminApi.updateCoupon(editing.value.id, payload);
    else await adminApi.createCoupon(payload);
    open.value = false;
    await execute();
    toast.success(editing.value ? "Coupon updated" : "Coupon created");
  } catch (e) {
    errors.value = fieldErrors(e);
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e);
  } finally {
    saving.value = false;
  }
}
async function deactivate(c) {
  busy.value = c.id;
  try {
    await adminApi.deactivateCoupon(c.id);
    await execute();
    toast.success("Coupon deactivated");
  } catch (e) {
    toast.error("Couldn't deactivate the coupon", errorMessage(e));
  } finally {
    busy.value = null;
  }
}
</script>

<template>
  <div>
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div><h1 class="text-xl font-semibold tracking-tight">Coupons</h1><p class="mt-1 text-sm text-ink-500">Discounts are validated on the server at checkout.</p></div>
      <ZButton @click="add"><Plus class="size-4" /> Create coupon</ZButton>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !data" class="mt-5 space-y-2"><div v-for="i in 4" :key="i" class="skeleton h-16 rounded-lg" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5"><ZEmptyState :icon="TicketPercent" title="No coupons yet" description="Create an offer to boost conversions." compact><ZButton @click="add">Create coupon</ZButton></ZEmptyState></div>
    <div v-else class="surface mt-5 overflow-x-auto">
      <table class="w-full min-w-[900px] text-sm">
        <thead class="border-b border-ink-150 text-left text-xs tracking-wide text-ink-500 uppercase">
          <tr><th class="p-4 font-semibold">Code</th><th class="p-4 font-semibold">Offer</th><th class="p-4 font-semibold">Scope</th><th class="p-4 font-semibold">Used</th><th class="p-4 font-semibold">Given</th><th class="p-4 font-semibold">Expires</th><th class="p-4" /></tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr v-for="c in data.content" :key="c.id" :class="busy === c.id && 'opacity-60'">
            <td class="p-4"><p class="font-mono font-bold">{{ c.code }}</p><ZBadge :tone="c.live ? 'success' : 'neutral'" size="sm" dot>{{ c.live ? 'Live' : c.active ? 'Scheduled or expired' : 'Inactive' }}</ZBadge></td>
            <td class="p-4">
              <p>{{ c.discountType === 'PERCENTAGE' ? `${c.discountValue}% off` : `${formatPrice(c.discountValue)} off` }}<span v-if="c.maxDiscount" class="text-ink-500"> up to {{ formatPrice(c.maxDiscount) }}</span></p>
              <p class="text-[13px] text-ink-500">{{ c.description }}</p>
            </td>
            <td class="p-4"><p>{{ c.scope === 'ALL' ? 'Everything' : c.scope === 'CATEGORY' ? c.categories.join(', ') : c.products.length + ' products' }}</p><p v-if="c.userSpecific" class="text-[13px] text-ink-500">Specific customers</p></td>
            <td class="tabular p-4">{{ c.usedCount }}<span v-if="c.usageLimit" class="text-ink-500">/{{ c.usageLimit }}</span></td>
            <td class="tabular p-4">{{ formatPrice(c.totalDiscountGiven) }}</td>
            <td class="p-4 text-ink-500">{{ c.expiresAt ? formatDate(c.expiresAt) : 'No expiry' }}</td>
            <td class="p-4 text-right">
              <div class="flex justify-end gap-1">
                <ZButton variant="ghost" size="sm" @click="edit(c)">Edit</ZButton>
                <ZButton v-if="c.active" variant="ghost" size="sm" @click="deactivate(c)">Deactivate</ZButton>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ZDialog v-model:open="open" :title="editing ? 'Edit coupon' : 'Create coupon'" size="md">
      <FormAlert v-if="formError" :message="formError" class="mb-4" />
      <form id="coupon-form" class="space-y-4" @submit.prevent="save">
        <ZInput v-model="form.code" label="Code" :disabled="!!editing" :error="errors.code" hint="4 to 20 letters and numbers" class="font-mono uppercase" />
        <ZInput v-model="form.description" label="Description" :error="errors.description" hint="Shown to shoppers with the offer" />
        <div class="grid gap-4 sm:grid-cols-2">
          <ZSelect v-model="form.discountType" label="Discount type" :options="[{ value: 'PERCENTAGE', label: 'Percentage off' }, { value: 'FIXED', label: 'Fixed amount off' }]" />
          <ZInput v-model.number="form.discountValue" label="Value" type="number" :error="errors.discountValue" :hint="form.discountType === 'PERCENTAGE' ? 'Up to 90%' : 'In rupees'" />
          <ZInput v-model.number="form.minOrderValue" label="Minimum order value" type="number" />
          <ZInput v-model.number="form.maxDiscount" label="Maximum discount" type="number" optional />
          <ZInput v-model.number="form.perUserLimit" label="Uses per customer" type="number" />
          <ZInput v-model.number="form.usageLimit" label="Total uses" type="number" optional hint="Leave empty for unlimited" />
        </div>
        <ZInput v-model="form.expiresAt" label="Expires on" type="date" optional />
        <ZCheckbox v-model="form.active" label="Active" />
      </form>
      <template #footer>
        <ZButton variant="ghost" @click="open = false">Cancel</ZButton>
        <ZButton type="submit" form="coupon-form" :loading="saving">{{ editing ? 'Save coupon' : 'Create coupon' }}</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
