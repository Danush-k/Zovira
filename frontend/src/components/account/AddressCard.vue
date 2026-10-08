<script setup>
import { Phone } from '@lucide/vue';
import ZBadge from '@/components/ui/ZBadge.vue';
defineProps({
    address: null,
    selectable: { type: Boolean, default: false },
    selected: { type: Boolean, default: false }
});
const typeLabel = { HOME: 'Home', WORK: 'Work', OTHER: 'Other' };
</script>

<template>
  <div class="min-w-0">
    <div class="flex flex-wrap items-center gap-2">
      <p class="font-semibold text-ink-900">{{ address.fullName }}</p>
      <ZBadge size="sm">{{ typeLabel[address.type] }}</ZBadge>
      <ZBadge v-if="address.isDefault" tone="brand" size="sm">Default</ZBadge>
    </div>
    <p class="mt-1.5 text-sm leading-relaxed text-ink-600">
      {{ address.line1 }}<template v-if="address.line2">, {{ address.line2 }}</template>
      <template v-if="address.landmark"><br />Near {{ address.landmark }}</template>
      <br />{{ address.city }}, {{ address.state }} {{ address.pincode }}
    </p>
    <p class="mt-1.5 flex items-center gap-1.5 text-sm text-ink-600">
      <Phone class="size-3.5 text-ink-400" />
      {{ address.phone }}
    </p>
  </div>
</template>
