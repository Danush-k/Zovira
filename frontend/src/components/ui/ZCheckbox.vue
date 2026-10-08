<script setup>
import { useId } from 'vue';
defineProps({
    label: null,
    description: null,
    disabled: { type: Boolean, default: false }
});
const model = defineModel({ default: false });
const id = useId();
</script>

<template>
  <label :for="id" :class="['group inline-flex items-start gap-2.5', disabled ? 'opacity-60' : 'cursor-pointer']">
    <input
      :id="id"
      v-model="model"
      type="checkbox"
      :disabled="disabled"
      class="mt-0.5 size-[18px] shrink-0 cursor-pointer rounded border-ink-300 accent-brand-700"
    />
    <span v-if="label || $slots.default" class="text-sm leading-snug text-ink-800">
      <slot>{{ label }}</slot>
      <span v-if="description" class="mt-0.5 block text-[13px] text-ink-500">{{ description }}</span>
    </span>
  </label>
</template>
