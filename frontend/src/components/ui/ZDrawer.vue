<script setup lang="ts">
import { ref, useId } from 'vue'
import { X } from '@lucide/vue'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useScrollLock } from '@/composables/useScrollLock'

withDefaults(defineProps<{ title?: string; side?: 'left' | 'right'; width?: string }>(), {
  side: 'right',
  width: 'max-w-md',
})

const open = defineModel<boolean>('open', { default: false })
const panel = ref<HTMLElement | null>(null)
const titleId = useId()

useFocusTrap(panel, open)
useScrollLock(open)
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200"
      enter-from-class="opacity-0"
      leave-active-class="transition duration-150"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-[65] bg-ink-950/40" aria-hidden="true" @click="open = false" />
    </Transition>
    <Transition
      :enter-from-class="side === 'right' ? 'translate-x-full' : '-translate-x-full'"
      enter-active-class="transition-transform duration-300 ease-[cubic-bezier(0.22,1,0.36,1)]"
      leave-active-class="transition-transform duration-200 ease-in"
      :leave-to-class="side === 'right' ? 'translate-x-full' : '-translate-x-full'"
    >
      <aside
        v-if="open"
        ref="panel"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="title ? titleId : undefined"
        tabindex="-1"
        :class="[
          'fixed inset-y-0 z-[66] flex w-full flex-col bg-white shadow-pop outline-none',
          width,
          side === 'right' ? 'right-0' : 'left-0',
        ]"
        @keydown.esc="open = false"
      >
        <header class="flex h-14 shrink-0 items-center justify-between border-b border-ink-100 px-4">
          <slot name="header">
            <h2 :id="titleId" class="text-base font-semibold text-ink-900">{{ title }}</h2>
          </slot>
          <button
            type="button"
            class="grid size-9 place-items-center rounded-lg text-ink-500 hover:bg-ink-100 hover:text-ink-900"
            aria-label="Close panel"
            @click="open = false"
          >
            <X class="size-5" />
          </button>
        </header>
        <div class="flex-1 overflow-y-auto">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="shrink-0 border-t border-ink-100 p-4">
          <slot name="footer" />
        </footer>
      </aside>
    </Transition>
  </Teleport>
</template>
