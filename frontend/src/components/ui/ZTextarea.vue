<script setup>
import { computed, useAttrs, useId } from 'vue';
defineOptions({ inheritAttrs: false });
const props = defineProps({
    label: null,
    hint: null,
    error: null,
    rows: { default: 4 },
    maxlength: null,
    optional: { type: Boolean, default: false }
});
const model = defineModel();
const attrs = useAttrs();
const autoId = useId();
const id = computed(() => attrs.id ?? autoId);
const count = computed(() => (model.value ?? '').length);
</script>

<template>
  <div :class="attrs.class">
    <label v-if="label" :for="id" class="field-label">
      {{ label }}
      <span v-if="optional" class="font-normal text-ink-400">(optional)</span>
    </label>
    <textarea
      v-bind="{ ...attrs, class: undefined }"
      :id="id"
      v-model="model"
      :rows="rows"
      :maxlength="maxlength"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="error ? `${id}-error` : undefined"
      class="field-control resize-y py-2.5 leading-relaxed"
    />
    <div class="mt-1.5 flex items-start justify-between gap-3 text-[13px]">
      <p v-if="error" :id="`${id}-error`" class="text-danger" role="alert">{{ error }}</p>
      <p v-else-if="hint" class="text-ink-500">{{ hint }}</p>
      <span v-else />
      <span v-if="props.maxlength" class="tabular shrink-0 text-ink-400">{{ count }}/{{ props.maxlength }}</span>
    </div>
  </div>
</template>
