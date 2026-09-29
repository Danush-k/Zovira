<script setup lang="ts">
import { ShieldOff } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
</script>

<template>
  <section class="container-page py-16">
    <ZEmptyState
      :icon="ShieldOff"
      title="You don't have access to this page"
      description="This area is only available to accounts with the right permissions."
    >
      <ZButton to="/">Go to homepage</ZButton>
      <ZButton to="/account" variant="outline">Your account</ZButton>
    </ZEmptyState>
  </section>
</template>
