<script setup lang="ts">
import { RouterView } from 'vue-router'
import OfflineBanner from '@/components/layout/OfflineBanner.vue'
import RouteProgress from '@/components/layout/RouteProgress.vue'
import ZToastHost from '@/components/ui/ZToastHost.vue'
</script>

<template>
  <RouteProgress />
  <OfflineBanner />
  <RouterView />
  <ZToastHost />
</template>
