<script setup lang="ts">
import { WifiOff } from '@lucide/vue'
import { useOnline } from '@/composables/useOnline'

const online = useOnline()
</script>

<template>
  <Transition
    enter-active-class="transition duration-200"
    enter-from-class="-translate-y-full"
    leave-active-class="transition duration-200"
    leave-to-class="-translate-y-full"
  >
    <div
      v-if="!online"
      class="sticky top-0 z-[60] flex items-center justify-center gap-2 bg-ink-900 px-4 py-2 text-center text-[13px] font-medium text-white"
      role="status"
    >
      <WifiOff class="size-4 shrink-0" />
      {{ $t('errors.offline') }}
    </div>
  </Transition>
</template>
