<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

/** Thin top progress bar shown while lazily loaded routes and their guards resolve. */
const router = useRouter()
const visible = ref(false)
const progress = ref(0)
let timer: ReturnType<typeof setInterval> | undefined
let showDelay: ReturnType<typeof setTimeout> | undefined

function start() {
  clearTimeout(showDelay)
  clearInterval(timer)
  progress.value = 8
  showDelay = setTimeout(() => {
    visible.value = true
    timer = setInterval(() => {
      progress.value = Math.min(90, progress.value + (90 - progress.value) * 0.12)
    }, 120)
  }, 120)
}

function done() {
  clearTimeout(showDelay)
  clearInterval(timer)
  if (!visible.value) return
  progress.value = 100
  setTimeout(() => {
    visible.value = false
    progress.value = 0
  }, 220)
}

const removeBefore = router.beforeEach(() => start())
const removeAfter = router.afterEach(() => done())
const removeError = router.onError(() => done())

onBeforeUnmount(() => {
  removeBefore()
  removeAfter()
  removeError()
  clearInterval(timer)
  clearTimeout(showDelay)
})
</script>

<template>
  <div
    v-show="visible"
    class="fixed inset-x-0 top-0 z-[90] h-0.5"
    role="progressbar"
    aria-label="Loading page"
    :aria-valuenow="Math.round(progress)"
  >
    <div class="h-full bg-accent-500 transition-[width] duration-200 ease-out" :style="{ width: `${progress}%` }" />
  </div>
</template>
