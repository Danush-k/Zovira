<script setup lang="ts">
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from '@lucide/vue'
import { useRouter } from 'vue-router'
import { useToastStore, type Toast } from '@/stores/toast'

const store = useToastStore()
const router = useRouter()

const icons = { success: CheckCircle2, error: AlertCircle, info: Info, warning: AlertTriangle }
const iconColors = {
  success: 'text-success',
  error: 'text-danger',
  info: 'text-info',
  warning: 'text-warning',
}

function runAction(toast: Toast) {
  if (toast.action?.handler) toast.action.handler()
  if (toast.action?.to) void router.push(toast.action.to)
  store.dismiss(toast.id)
}
</script>

<template>
  <div
    class="pointer-events-none fixed inset-x-0 bottom-[calc(4.5rem+env(safe-area-inset-bottom))] z-[80] flex flex-col items-center gap-2 px-4 sm:inset-x-auto sm:top-5 sm:right-5 sm:bottom-auto sm:items-end"
    aria-live="polite"
    aria-atomic="false"
  >
    <TransitionGroup
      enter-active-class="transition duration-250 ease-[cubic-bezier(0.22,1,0.36,1)]"
      enter-from-class="opacity-0 translate-y-2 sm:translate-y-0 sm:translate-x-4"
      leave-active-class="transition duration-150 ease-in"
      leave-to-class="opacity-0"
      move-class="transition duration-200"
    >
      <div
        v-for="toast in store.toasts"
        :key="toast.id"
        :role="toast.tone === 'error' ? 'alert' : 'status'"
        class="pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl border border-ink-150 bg-white p-3.5 pr-2.5 shadow-pop"
      >
        <component :is="icons[toast.tone]" :class="['mt-0.5 size-5 shrink-0', iconColors[toast.tone]]" />
        <div class="min-w-0 flex-1">
          <p class="text-sm font-semibold text-ink-900">{{ toast.title }}</p>
          <p v-if="toast.message" class="mt-0.5 text-[13px] text-ink-600">{{ toast.message }}</p>
          <button
            v-if="toast.action"
            type="button"
            class="mt-1.5 text-[13px] font-semibold text-brand-700 hover:underline"
            @click="runAction(toast)"
          >
            {{ toast.action.label }}
          </button>
        </div>
        <button
          type="button"
          class="grid size-7 shrink-0 place-items-center rounded-md text-ink-400 hover:bg-ink-100 hover:text-ink-700"
          aria-label="Dismiss notification"
          @click="store.dismiss(toast.id)"
        >
          <X class="size-4" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>
