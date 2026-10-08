<script setup>
import { ref, watch } from "vue";
import { adminApi } from "@/services/admin";
import { useAsync } from "@/composables/useAsync";
const page = ref(0);
const { data, loading, error, execute } = useAsync((p) => adminApi.auditLog(p));
watch(page, (p) => void execute(p), { immediate: true });
const label = (action) => action.replace(/_/g, " ").toLowerCase().replace(/^./, (c) => c.toUpperCase());
</script>

<template>
  <div>
    <h1 class="text-xl font-semibold tracking-tight">Audit log</h1>
    <p class="mt-1 text-sm text-ink-500">Security and administrative events, newest first.</p>

    <ZErrorState v-if="error" :error="error" @retry="execute(page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-2"><div v-for="i in 8" :key="i" class="skeleton h-12 rounded-lg" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5"><ZEmptyState :icon="FileClock" title="Nothing logged yet" description="Administrative actions are recorded here." compact /></div>
    <div v-else class="surface mt-5 overflow-x-auto">
      <table class="w-full min-w-[760px] text-sm">
        <thead class="border-b border-ink-150 text-left text-xs tracking-wide text-ink-500 uppercase">
          <tr><th class="p-4 font-semibold">When</th><th class="p-4 font-semibold">Actor</th><th class="p-4 font-semibold">Action</th><th class="p-4 font-semibold">Target</th><th class="p-4 font-semibold">Details</th></tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr v-for="a in data.content" :key="a.id">
            <td class="p-4 whitespace-nowrap text-ink-500">{{ formatDateTime(a.createdAt) }}</td>
            <td class="p-4">{{ a.actorEmail ?? 'System' }}<p v-if="a.ipAddress" class="font-mono text-[12px] text-ink-400">{{ a.ipAddress }}</p></td>
            <td class="p-4 font-medium">{{ label(a.action) }}</td>
            <td class="p-4 text-ink-600">{{ a.entityType }}<span v-if="a.entityId" class="font-mono"> #{{ a.entityId }}</span></td>
            <td class="p-4 font-mono text-[12px] text-ink-500">{{ a.details ? JSON.stringify(a.details) : '' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />
  </div>
</template>
