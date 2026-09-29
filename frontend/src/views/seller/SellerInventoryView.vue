<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Boxes, Save } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZSwitch from '@/components/ui/ZSwitch.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import { sellerApi } from '@/services/seller'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useToastStore } from '@/stores/toast'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const toast = useToastStore()
const lowOnly = ref(route.query.low === 'true')
const { data: rows, loading, error, execute } = useAsync((low: boolean) => sellerApi.inventory(low))
watch(lowOnly, (v) => void execute(v), { immediate: true })

/** Only edited rows are sent, so a slow save never overwrites someone else's untouched stock. */
const edits = ref<Record<number, number>>({})
const saving = ref(false)
const dirty = computed(() => Object.keys(edits.value).length)

function onInput(variantId: number, value: string, original: number) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0) return
  if (parsed === original) delete edits.value[variantId]
  else edits.value[variantId] = parsed
  edits.value = { ...edits.value }
}

async function save() {
  saving.value = true
  try {
    await sellerApi.updateStock(Object.entries(edits.value).map(([variantId, available]) => ({ variantId: Number(variantId), available })))
    edits.value = {}
    await execute(lowOnly.value)
    toast.success('Stock updated')
  } catch (e) {
    toast.error("Couldn't update stock", errorMessage(e))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold tracking-tight">Inventory</h1>
        <p class="mt-1 text-sm text-ink-500">Stock available to sell, across every option.</p>
      </div>
      <div class="flex items-center gap-4">
        <ZSwitch v-model="lowOnly" label="Low stock only" />
        <ZButton :disabled="!dirty" :loading="saving" @click="save"><Save class="size-4" /> Save {{ dirty ? `(${dirty})` : '' }}</ZButton>
      </div>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute(lowOnly)" />
    <div v-else-if="loading && !rows" class="mt-5 space-y-2"><div v-for="i in 6" :key="i" class="skeleton h-14 rounded-lg" /></div>
    <div v-else-if="!rows?.length" class="surface mt-5">
      <ZEmptyState :icon="Boxes" :title="lowOnly ? 'Nothing running low' : 'No stock records yet'" description="Add a product to start tracking stock." compact />
    </div>
    <div v-else class="surface mt-5 overflow-x-auto">
      <table class="w-full min-w-[760px] text-sm">
        <thead class="border-b border-ink-150 text-left text-xs tracking-wide text-ink-500 uppercase">
          <tr><th class="p-4 font-semibold">Product</th><th class="p-4 font-semibold">SKU</th><th class="p-4 font-semibold">Price</th><th class="p-4 font-semibold">Reserved</th><th class="p-4 font-semibold">Available</th></tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr v-for="row in rows" :key="row.variantId" :class="!row.active && 'opacity-50'">
            <td class="p-4">
              <div class="flex items-center gap-3">
                <img v-if="row.imageUrl" :src="row.imageUrl" alt="" class="size-10 shrink-0 rounded-lg bg-ink-50 object-contain p-1 mix-blend-multiply" />
                <div class="min-w-0">
                  <p class="line-clamp-1 font-medium">{{ row.productTitle }}</p>
                  <p class="text-[13px] text-ink-500">{{ row.variantName }}</p>
                </div>
              </div>
            </td>
            <td class="p-4 font-mono text-[13px] text-ink-600">{{ row.sku }}</td>
            <td class="tabular p-4">{{ formatPrice(row.price) }}</td>
            <td class="tabular p-4 text-ink-500">{{ row.reserved }}</td>
            <td class="p-4">
              <input
                type="number"
                min="0"
                :value="edits[row.variantId] ?? row.available"
                :aria-label="`Available stock for ${row.productTitle} ${row.variantName}`"
                :class="['field-control tabular h-9 w-24', edits[row.variantId] !== undefined && 'border-brand-500 ring-3 ring-brand-500/15', row.available === 0 && edits[row.variantId] === undefined && 'text-danger']"
                @input="onInput(row.variantId, ($event.target as HTMLInputElement).value, row.available)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
