<script setup lang="ts">
import { computed, ref, useId } from 'vue'
import { X } from '@lucide/vue'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useScrollLock } from '@/composables/useScrollLock'

const props = withDefaults(
  defineProps<{
    title?: string
    description?: string
    size?: 'sm' | 'md' | 'lg' | 'xl'
    hideClose?: boolean
    dismissible?: boolean
  }>(),
  { size: 'md', dismissible: true },
)

const open = defineModel<boolean>('open', { default: false })
const panel = ref<HTMLElement | null>(null)
const titleId = useId()
const descriptionId = useId()

useFocusTrap(panel, open)
useScrollLock(open)

const sizes = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' }
const panelClass = computed(() => sizes[props.size])

function close() {
  if (props.dismissible) open.value = false
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      leave-active-class="transition duration-150 ease-in"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[70] flex items-end justify-center bg-ink-950/45 p-0 backdrop-blur-[2px] sm:items-center sm:p-6"
        @mousedown.self="close"
        @keydown.esc.stop="close"
      >
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="title ? titleId : undefined"
          :aria-describedby="description ? descriptionId : undefined"
          tabindex="-1"
          :class="[
            'relative flex max-h-[92vh] w-full animate-pop-in flex-col overflow-hidden rounded-t-2xl bg-white shadow-pop outline-none sm:rounded-2xl',
            panelClass,
          ]"
        >
          <header v-if="title || !hideClose" class="flex items-start justify-between gap-4 px-6 pt-5 pb-1">
            <div>
              <h2 v-if="title" :id="titleId" class="text-lg font-semibold tracking-tight text-ink-900">{{ title }}</h2>
              <p v-if="description" :id="descriptionId" class="mt-1 text-sm text-ink-500">{{ description }}</p>
            </div>
            <button
              v-if="!hideClose"
              type="button"
              class="-mt-1 -mr-2 grid size-9 shrink-0 place-items-center rounded-lg text-ink-500 hover:bg-ink-100 hover:text-ink-900"
              aria-label="Close dialog"
              @click="open = false"
            >
              <X class="size-5" />
            </button>
          </header>
          <div class="overflow-y-auto px-6 py-4">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="flex flex-wrap justify-end gap-2 border-t border-ink-100 bg-ink-50/60 px-6 py-4">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
