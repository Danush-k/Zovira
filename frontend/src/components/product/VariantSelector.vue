<script setup lang="ts">
import { Check } from '@lucide/vue'
import type { ProductAttribute, ProductVariant } from '@/types/catalog'
import { choose, optionState, type Selection } from '@/utils/variants'

const props = defineProps<{ attributes: ProductAttribute[]; variants: ProductVariant[] }>()
const selection = defineModel<Selection>({ required: true })

function pick(attribute: string, value: string) {
  selection.value = choose(props.variants, props.attributes, selection.value, attribute, value)
}
</script>

<template>
  <div class="space-y-5">
    <fieldset v-for="attribute in attributes" :key="attribute.name">
      <legend class="mb-2.5 text-sm text-ink-600">
        {{ attribute.name }}:
        <span class="font-semibold text-ink-900">{{ selection[attribute.name] }}</span>
      </legend>
      <div class="flex flex-wrap gap-2">
        <template v-for="option in attribute.options" :key="option.value">
          <button
            v-if="option.swatch"
            type="button"
            :aria-label="`${attribute.name}: ${option.value}`"
            :aria-pressed="selection[attribute.name] === option.value"
            :title="option.value"
            :class="[
              'relative grid size-10 place-items-center rounded-full ring-offset-2 transition',
              selection[attribute.name] === option.value ? 'ring-2 ring-brand-700' : 'ring-1 ring-ink-200 hover:ring-ink-400',
              optionState(variants, selection, attribute.name, option.value) !== 'available' && 'opacity-40',
            ]"
            @click="pick(attribute.name, option.value)"
          >
            <span class="size-8 rounded-full border border-black/10" :style="{ backgroundColor: option.swatch }" />
            <Check
              v-if="selection[attribute.name] === option.value"
              class="absolute size-4 text-white mix-blend-difference"
              stroke-width="3"
            />
          </button>
          <button
            v-else
            type="button"
            :aria-pressed="selection[attribute.name] === option.value"
            :class="[
              'relative min-w-12 rounded-lg border px-3.5 py-2 text-sm font-semibold transition-colors',
              selection[attribute.name] === option.value
                ? 'border-brand-700 bg-brand-50 text-brand-800'
                : 'border-ink-200 bg-white text-ink-800 hover:border-ink-400',
              optionState(variants, selection, attribute.name, option.value) === 'out-of-stock' && 'text-ink-400 line-through decoration-ink-300',
              optionState(variants, selection, attribute.name, option.value) === 'unavailable' && 'border-dashed text-ink-400',
            ]"
            @click="pick(attribute.name, option.value)"
          >
            {{ option.value }}
          </button>
        </template>
      </div>
    </fieldset>
  </div>
</template>
