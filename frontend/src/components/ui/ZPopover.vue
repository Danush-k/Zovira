<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useClickOutside } from '@/composables/useClickOutside'

const props = withDefaults(
  defineProps<{
    align?: 'start' | 'end'
    width?: string
    hover?: boolean
    label?: string
  }>(),
  { align: 'end', width: 'w-72' },
)

const open = defineModel<boolean>('open', { default: false })
const root = ref<HTMLElement | null>(null)
const panel = ref<HTMLElement | null>(null)
let hoverTimer: ReturnType<typeof setTimeout> | undefined

useClickOutside([root], () => (open.value = false))

function menuItems(): HTMLElement[] {
  return Array.from(panel.value?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])
}

function onPanelKeydown(e: KeyboardEvent) {
  const items = menuItems()
  if (!items.length) return
  const index = items.indexOf(document.activeElement as HTMLElement)
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    items[(index + 1) % items.length]?.focus()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    items[(index - 1 + items.length) % items.length]?.focus()
  }
}

function onEnter() {
  if (!props.hover) return
  clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => (open.value = true), 80)
}

function onLeave() {
  if (!props.hover) return
  clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => (open.value = false), 140)
}

watch(open, async (value) => {
  if (value) {
    await nextTick()
    menuItems()[0]?.setAttribute('tabindex', '0')
  }
})

defineExpose({ close: () => (open.value = false) })
</script>

<template>
  <div ref="root" class="relative" @mouseenter="onEnter" @mouseleave="onLeave" @keydown.esc="open = false">
    <slot name="trigger" :open="open" :toggle="() => (open = !open)" />
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 translate-y-1"
      leave-active-class="transition duration-100 ease-in"
      leave-to-class="opacity-0 translate-y-1"
    >
      <div
        v-if="open"
        ref="panel"
        :aria-label="label"
        :class="[
          'absolute top-full z-50 mt-2 overflow-hidden rounded-xl border border-ink-150 bg-white shadow-pop',
          width,
          align === 'end' ? 'right-0' : 'left-0',
        ]"
        @keydown="onPanelKeydown"
      >
        <slot :close="() => (open = false)" />
      </div>
    </Transition>
  </div>
</template>
