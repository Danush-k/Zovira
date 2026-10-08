<script setup>
import { ref, watch } from "vue";
import { adminApi } from "@/services/admin";
import { errorMessage } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useDebouncedRef } from "@/composables/useDebounce";
import { useToastStore } from "@/stores/toast";
const status = ref("ALL");
const search = ref("");
const debounced = useDebouncedRef(search, 300);
const page = ref(0);
const toast = useToastStore();
const { data, loading, error, execute } = useAsync(
  (s, q, p) => adminApi.orders({ status: s, q, page: p, size: 20 })
);
watch([status, debounced, page], () => void execute(status.value, debounced.value, page.value), { immediate: true });
watch([status, debounced], () => page.value = 0);
const tabs = [
  { value: "ALL", label: "All" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
  { value: "PENDING_PAYMENT", label: "Unpaid" }
];
const target = ref(null);
const reason = ref("");
const busy = ref(false);
async function cancel() {
  if (!target.value) return;
  busy.value = true;
  try {
    await adminApi.cancelOrder(target.value.orderNumber, reason.value);
    target.value = null;
    reason.value = "";
    await execute(status.value, debounced.value, page.value);
    toast.success("Order cancelled", "Any payment has been refunded and stock restored.");
  } catch (e) {
    toast.error("Couldn't cancel the order", errorMessage(e));
  } finally {
    busy.value = false;
  }
}
const cancellable = (s) => ["PENDING_PAYMENT", "PLACED", "CONFIRMED", "PROCESSING"].includes(s);
</script>

<template>
  <div>
    <h1 class="text-xl font-semibold tracking-tight">Orders</h1>
    <p class="mt-1 text-sm text-ink-500">Every order placed on Zovira.</p>

    <div class="mt-5 flex flex-wrap items-center gap-3">
      <ZTabs v-model="status" :tabs="tabs" label="Filter orders" class="flex-1" />
      <div class="relative w-full sm:w-64">
        <label for="admin-order-search" class="sr-only">Search orders</label>
        <Search class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-400" />
        <input id="admin-order-search" v-model="search" type="search" placeholder="Order number or email" class="field-control h-9 pl-9 text-sm" />
      </div>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute(status, debounced, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-2"><div v-for="i in 6" :key="i" class="skeleton h-14 rounded-lg" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5"><ZEmptyState :icon="ShoppingBag" title="No orders found" description="Try a different filter." compact /></div>
    <div v-else class="surface mt-5 overflow-x-auto">
      <table class="w-full min-w-[900px] text-sm">
        <thead class="border-b border-ink-150 text-left text-xs tracking-wide text-ink-500 uppercase">
          <tr><th class="p-4 font-semibold">Order</th><th class="p-4 font-semibold">Customer</th><th class="p-4 font-semibold">Status</th><th class="p-4 font-semibold">Payment</th><th class="p-4 font-semibold">Total</th><th class="p-4 font-semibold">Placed</th><th class="p-4" /></tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr v-for="o in data.content" :key="o.orderNumber">
            <td class="p-4"><p class="font-mono font-semibold">{{ o.orderNumber }}</p><p class="text-[13px] text-ink-500">{{ o.itemCount }} items · {{ o.city }}</p></td>
            <td class="p-4"><p class="font-medium">{{ o.customerName }}</p><p class="text-[13px] text-ink-500">{{ o.customerEmail }}</p></td>
            <td class="p-4"><OrderStatusBadge :status="o.status" /></td>
            <td class="p-4"><p>{{ o.paymentMethod }}</p><p class="text-[13px] text-ink-500">{{ o.paymentStatus }}</p></td>
            <td class="tabular p-4"><p class="font-semibold">{{ formatPrice(o.totalAmount) }}</p><p v-if="o.refundedAmount > 0" class="text-[13px] text-success">-{{ formatPrice(o.refundedAmount) }} refunded</p></td>
            <td class="p-4 text-ink-500">{{ formatDate(o.placedAt) }}</td>
            <td class="p-4 text-right"><ZButton v-if="cancellable(o.status)" variant="ghost" size="sm" @click="target = o">Cancel</ZButton></td>
          </tr>
        </tbody>
      </table>
    </div>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />

    <ZDialog :open="!!target" title="Cancel this order?" description="The customer is refunded and stock goes back on sale." size="sm" @update:open="(v) => !v && (target = null)">
      <p v-if="target" class="mb-4 font-mono text-sm font-semibold">{{ target.orderNumber }}</p>
      <ZTextarea v-model="reason" label="Reason" :rows="3" :maxlength="200" hint="Shown to the customer" />
      <template #footer>
        <ZButton variant="ghost" @click="target = null">Keep order</ZButton>
        <ZButton variant="danger" :loading="busy" @click="cancel">Cancel order</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
