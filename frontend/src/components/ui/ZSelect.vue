<script setup>
import { computed, useAttrs, useId } from 'vue';
import { ChevronDown } from '@lucide/vue';
defineOptions({ inheritAttrs: false });
defineProps({
    label: null,
    options: null,
    placeholder: null,
    error: null,
    hint: null,
    size: null
});
const model = defineModel();
const attrs = useAttrs();
const autoId = useId();
const id = computed(() => attrs.id ?? autoId);
</script>

<template>
  <div :class="attrs.class">
    <label v-if="label" :for="id" class="field-label">{{ label }}</label>
    <div class="relative">
      <select
        v-bind="{ ...attrs, class: undefined }"
        :id="id"
        v-model="model"
        :aria-invalid="error ? 'true' : undefined"
        :class="['field-control appearance-none pr-9', size === 'sm' ? 'h-8 text-[13px]' : 'h-10']"
      >
        <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
        <option v-for="opt in options" :key="opt.value" :value="opt.value" :disabled="opt.disabled">
          {{ opt.label }}
        </option>
      </select>
      <ChevronDown
        class="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-ink-500"
        aria-hidden="true"
      />
    </div>
    <p v-if="error" class="mt-1.5 text-[13px] text-danger" role="alert">{{ error }}</p>
    <p v-else-if="hint" class="mt-1.5 text-[13px] text-ink-500">{{ hint }}</p>
  </div>
</template>
