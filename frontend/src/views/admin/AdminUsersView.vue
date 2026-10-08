<script setup>
import { ref, watch } from "vue";
import { adminApi } from "@/services/admin";
import { errorMessage } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useDebouncedRef } from "@/composables/useDebounce";
import { useToastStore } from "@/stores/toast";
const role = ref("ALL");
const search = ref("");
const debounced = useDebouncedRef(search, 300);
const page = ref(0);
const toast = useToastStore();
const { data, loading, error, execute } = useAsync(
  (r, q, p) => adminApi.users({ role: r, q, page: p, size: 20 })
);
watch([role, debounced, page], () => void execute(role.value, debounced.value, page.value), { immediate: true });
watch([role, debounced], () => page.value = 0);
const tabs = [
  { value: "ALL", label: "Everyone" },
  { value: "CUSTOMER", label: "Customers" },
  { value: "SELLER", label: "Sellers" },
  { value: "ADMIN", label: "Admins" }
];
const target = ref(null);
const reason = ref("");
const busy = ref(false);
async function confirmSuspend() {
  if (!target.value) return;
  busy.value = true;
  try {
    await adminApi.setUserStatus(target.value.id, "SUSPENDED", reason.value);
    target.value = null;
    reason.value = "";
    await execute(role.value, debounced.value, page.value);
    toast.success("Account suspended", "All their sessions have been signed out.");
  } catch (e) {
    toast.error("Couldn't suspend the account", errorMessage(e));
  } finally {
    busy.value = false;
  }
}
async function reactivate(user) {
  try {
    await adminApi.setUserStatus(user.id, "ACTIVE");
    await execute(role.value, debounced.value, page.value);
    toast.success("Account reactivated");
  } catch (e) {
    toast.error("Couldn't reactivate the account", errorMessage(e));
  }
}
</script>

<template>
  <div>
    <h1 class="text-xl font-semibold tracking-tight">Customers</h1>
    <p class="mt-1 text-sm text-ink-500">Every account on Zovira.</p>

    <div class="mt-5 flex flex-wrap items-center gap-3">
      <ZTabs v-model="role" :tabs="tabs" label="Filter by role" class="flex-1" />
      <div class="relative w-full sm:w-64">
        <label for="admin-user-search" class="sr-only">Search users</label>
        <Search class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-400" />
        <input id="admin-user-search" v-model="search" type="search" placeholder="Name or email" class="field-control h-9 pl-9 text-sm" />
      </div>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute(role, debounced, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-2"><div v-for="i in 6" :key="i" class="skeleton h-14 rounded-lg" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5"><ZEmptyState :icon="Users" title="No accounts found" description="Try a different search or filter." compact /></div>
    <div v-else class="surface mt-5 overflow-x-auto">
      <table class="w-full min-w-[820px] text-sm">
        <thead class="border-b border-ink-150 text-left text-xs tracking-wide text-ink-500 uppercase">
          <tr><th class="p-4 font-semibold">Account</th><th class="p-4 font-semibold">Roles</th><th class="p-4 font-semibold">Orders</th><th class="p-4 font-semibold">Joined</th><th class="p-4 font-semibold">Status</th><th class="p-4" /></tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr v-for="u in data.content" :key="u.id">
            <td class="p-4">
              <p class="font-medium">{{ u.fullName }}</p>
              <p class="text-[13px] text-ink-500">{{ u.email }}<span v-if="!u.emailVerified" class="ml-1 text-warning">(unverified)</span></p>
            </td>
            <td class="p-4"><span class="flex flex-wrap gap-1"><ZBadge v-for="r in u.roles" :key="r" size="sm" :tone="r === 'ADMIN' ? 'dark' : r === 'SELLER' ? 'brand' : 'neutral'">{{ r.toLowerCase() }}</ZBadge></span></td>
            <td class="tabular p-4">{{ u.orderCount }}</td>
            <td class="p-4 text-ink-500">{{ formatDate(u.createdAt) }}</td>
            <td class="p-4"><ZBadge :tone="u.status === 'ACTIVE' ? 'success' : 'danger'" size="sm" dot>{{ u.status === 'ACTIVE' ? 'Active' : 'Suspended' }}</ZBadge></td>
            <td class="p-4 text-right">
              <ZButton v-if="u.status === 'ACTIVE' && !u.roles.includes('ADMIN')" variant="ghost" size="sm" @click="target = u">Suspend</ZButton>
              <ZButton v-else-if="u.status === 'SUSPENDED'" variant="ghost" size="sm" @click="reactivate(u)">Reactivate</ZButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />

    <ZDialog :open="!!target" title="Suspend this account?" description="They will be signed out immediately and unable to sign back in." size="sm" @update:open="(v) => !v && (target = null)">
      <p v-if="target" class="mb-4 text-sm font-medium">{{ target.fullName }} · {{ target.email }}</p>
      <ZTextarea v-model="reason" label="Reason" :rows="3" :maxlength="500" hint="Recorded in the audit log" />
      <template #footer>
        <ZButton variant="ghost" @click="target = null">Cancel</ZButton>
        <ZButton variant="danger" :loading="busy" @click="confirmSuspend">Suspend account</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
