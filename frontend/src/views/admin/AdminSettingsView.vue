<script setup>
import { ref, watch } from "vue";
import { adminApi } from "@/services/admin";
import { errorMessage } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useToastStore } from "@/stores/toast";
const { data: settings, loading, error, execute } = useAsync(adminApi.settings, { immediate: true });
const toast = useToastStore();
const edits = ref({});
const saving = ref(false);
watch(settings, () => edits.value = {});
const groups = [
  { title: "Delivery", prefix: "shipping." },
  { title: "Payments", prefix: "payments." },
  { title: "Orders", prefix: "orders." },
  { title: "Reviews", prefix: "reviews." }
];
const isBoolean = (value) => value === "true" || value === "false";
async function save() {
  saving.value = true;
  try {
    settings.value = await adminApi.updateSettings(edits.value);
    edits.value = {};
    toast.success("Settings saved", "The storefront picks these up within a few minutes.");
  } catch (e) {
    toast.error("Couldn't save settings", errorMessage(e));
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="max-w-3xl">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div><h1 class="text-xl font-semibold tracking-tight">Platform settings</h1><p class="mt-1 text-sm text-ink-500">Delivery fees, payment rules and order limits.</p></div>
      <ZButton :disabled="!Object.keys(edits).length" :loading="saving" @click="save"><Save class="size-4" /> Save changes</ZButton>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !settings" class="mt-5 space-y-2"><div v-for="i in 6" :key="i" class="skeleton h-16 rounded-lg" /></div>
    <div v-else-if="settings" class="mt-5 space-y-5">
      <section v-for="group in groups" :key="group.prefix" class="surface p-5">
        <h2 class="font-semibold">{{ group.title }}</h2>
        <dl class="mt-3 divide-y divide-ink-100">
          <div v-for="s in settings.filter((x) => x.key.startsWith(group.prefix))" :key="s.key" class="flex flex-wrap items-center gap-4 py-3">
            <div class="min-w-0 flex-1">
              <dt class="text-sm font-medium">{{ s.description ?? s.key }}</dt>
              <dd class="font-mono text-[12px] text-ink-400">{{ s.key }} · updated {{ formatDateTime(s.updatedAt) }}</dd>
            </div>
            <div class="w-40">
              <select v-if="isBoolean(s.value)" :value="edits[s.key] ?? s.value" class="field-control h-9 text-sm" :aria-label="s.description ?? s.key" @change="edits = { ...edits, [s.key]: $event.target.value }">
                <option value="true">Enabled</option>
                <option value="false">Disabled</option>
              </select>
              <input v-else :value="edits[s.key] ?? s.value" :aria-label="s.description ?? s.key" class="field-control tabular h-9 text-sm" @input="edits = { ...edits, [s.key]: $event.target.value }" />
            </div>
          </div>
        </dl>
      </section>
    </div>
  </div>
</template>
