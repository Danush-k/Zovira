<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import { adminApi } from "@/services/admin";
import { errorMessage } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useToastStore } from "@/stores/toast";
const route = useRoute();
const status = ref(typeof route.query.status === "string" ? route.query.status : "ALL");
const page = ref(0);
const toast = useToastStore();
const { data, loading, error, execute } = useAsync((s, p) => adminApi.sellers({ status: s, page: p, size: 20 }));
watch([status, page], () => void execute(status.value, page.value), { immediate: true });
watch(status, () => page.value = 0);
const tabs = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Applications" },
  { value: "APPROVED", label: "Approved" },
  { value: "SUSPENDED", label: "Suspended" },
  { value: "REJECTED", label: "Rejected" }
];
const tones = { APPROVED: "success", PENDING: "warning", REJECTED: "danger", SUSPENDED: "danger" };
const target = ref(null);
const action = ref("REJECTED");
const reason = ref("");
const busy = ref(null);
async function approve(seller) {
  busy.value = seller.id;
  try {
    await adminApi.decideSeller(seller.id, "APPROVED");
    await execute(status.value, page.value);
    toast.success("Seller approved", `${seller.storeName} can now list products.`);
  } catch (e) {
    toast.error("Couldn't approve the seller", errorMessage(e));
  } finally {
    busy.value = null;
  }
}
function open(seller, next) {
  target.value = seller;
  action.value = next;
  reason.value = "";
}
async function confirm() {
  if (!target.value) return;
  busy.value = target.value.id;
  try {
    await adminApi.decideSeller(target.value.id, action.value, reason.value);
    target.value = null;
    await execute(status.value, page.value);
    toast.success(action.value === "REJECTED" ? "Application rejected" : "Seller suspended", "Their listings are no longer visible.");
  } catch (e) {
    toast.error("Couldn't update the seller", errorMessage(e));
  } finally {
    busy.value = null;
  }
}
</script>

<template>
  <div>
    <h1 class="text-xl font-semibold tracking-tight">Sellers</h1>
    <p class="mt-1 text-sm text-ink-500">Review applications and manage seller accounts.</p>
    <ZTabs v-model="status" :tabs="tabs" label="Filter sellers" class="mt-5" />

    <ZErrorState v-if="error" :error="error" @retry="execute(status, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="skeleton h-28 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5"><ZEmptyState :icon="Store" title="No sellers here" description="Applications appear here as they arrive." compact /></div>
    <ul v-else class="mt-5 space-y-3">
      <li v-for="s in data.content" :key="s.id" class="surface flex flex-wrap items-start gap-4 p-5">
        <span class="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700"><Store class="size-5" stroke-width="1.75" /></span>
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <p class="font-semibold">{{ s.storeName }}</p>
            <ZBadge :tone="tones[s.status]" size="sm" dot>{{ s.status.charAt(0) + s.status.slice(1).toLowerCase() }}</ZBadge>
            <ZRatingPill v-if="s.ratingCount" :value="s.ratingAverage" :count="s.ratingCount" />
          </div>
          <p class="mt-1 text-[13px] text-ink-600">{{ s.ownerName }} · {{ s.ownerEmail }}</p>
          <p class="text-[13px] text-ink-500">
            <template v-if="s.city">{{ s.city }}, {{ s.state }} · </template>
            <template v-if="s.gstin">GSTIN {{ s.gstin }} · </template>
            Applied {{ formatDate(s.appliedAt) }}
          </p>
          <p v-if="s.statusReason" class="mt-1 text-[13px] text-danger">{{ s.statusReason }}</p>
        </div>
        <div class="text-right text-[13px]">
          <p class="text-ink-500">{{ s.productCount }} listings</p>
          <p class="tabular font-semibold">{{ formatPrice(s.lifetimeRevenue) }}</p>
          <div class="mt-2 flex flex-wrap justify-end gap-1">
            <template v-if="s.status === 'PENDING'">
              <ZButton size="sm" :loading="busy === s.id" @click="approve(s)">Approve</ZButton>
              <ZButton size="sm" variant="ghost" @click="open(s, 'REJECTED')">Reject</ZButton>
            </template>
            <template v-else-if="s.status === 'APPROVED'">
              <ZButton :to="`/store/${s.slug}`" size="sm" variant="ghost">Storefront</ZButton>
              <ZButton size="sm" variant="ghost" @click="open(s, 'SUSPENDED')">Suspend</ZButton>
            </template>
            <ZButton v-else size="sm" variant="ghost" :loading="busy === s.id" @click="approve(s)">Reinstate</ZButton>
          </div>
        </div>
      </li>
    </ul>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />

    <ZDialog :open="!!target" :title="action === 'REJECTED' ? 'Reject this application?' : 'Suspend this seller?'" description="They will be told the reason you give here." size="sm" @update:open="(v) => !v && (target = null)">
      <p v-if="target" class="mb-4 text-sm font-medium">{{ target.storeName }}</p>
      <ZTextarea v-model="reason" label="Reason" :rows="3" :maxlength="500" />
      <template #footer>
        <ZButton variant="ghost" @click="target = null">Cancel</ZButton>
        <ZButton variant="danger" :loading="busy !== null" @click="confirm">{{ action === 'REJECTED' ? 'Reject' : 'Suspend' }}</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
