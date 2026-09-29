<script setup lang="ts">
import { computed, ref, useAttrs, useId, type Component } from 'vue'
import { Eye, EyeOff } from '@lucide/vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    label?: string
    hint?: string
    error?: string | null
    type?: string
    icon?: Component
    size?: 'md' | 'lg'
    optional?: boolean
  }>(),
  { type: 'text', size: 'md' },
)

const model = defineModel<string | number | null>()
const attrs = useAttrs()
const autoId = useId()
const id = computed(() => (attrs.id as string | undefined) ?? autoId)
const showPassword = ref(false)
const inputType = computed(() => (props.type === 'password' && showPassword.value ? 'text' : props.type))
const describedBy = computed(() => (props.error ? `${id.value}-error` : props.hint ? `${id.value}-hint` : undefined))
</script>

<template>
  <div :class="attrs.class">
    <label v-if="label" :for="id" class="field-label">
      {{ label }}
      <span v-if="optional" class="font-normal text-ink-400">(optional)</span>
    </label>
    <div class="relative">
      <component
        :is="icon"
        v-if="icon"
        class="pointer-events-none absolute top-1/2 left-3 size-[18px] -translate-y-1/2 text-ink-400"
        aria-hidden="true"
      />
      <input
        v-bind="{ ...attrs, class: undefined }"
        :id="id"
        v-model="model"
        :type="inputType"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
        :class="[
          'field-control',
          size === 'lg' ? 'h-12' : 'h-10',
          icon && 'pl-10',
          type === 'password' && 'pr-11',
        ]"
      />
      <button
        v-if="type === 'password'"
        type="button"
        class="absolute top-1/2 right-1.5 grid size-8 -translate-y-1/2 place-items-center rounded-md text-ink-500 hover:bg-ink-100 hover:text-ink-800"
        :aria-label="showPassword ? 'Hide password' : 'Show password'"
        :aria-pressed="showPassword"
        @click="showPassword = !showPassword"
      >
        <EyeOff v-if="showPassword" class="size-[18px]" />
        <Eye v-else class="size-[18px]" />
      </button>
    </div>
    <p v-if="error" :id="`${id}-error`" class="mt-1.5 text-[13px] text-danger" role="alert">{{ error }}</p>
    <p v-else-if="hint" :id="`${id}-hint`" class="mt-1.5 text-[13px] text-ink-500">{{ hint }}</p>
  </div>
</template>
