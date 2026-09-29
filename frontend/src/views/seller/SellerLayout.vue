<script setup lang="ts">
import { BarChart3, Boxes, LayoutDashboard, Package, RotateCcw, Store } from '@lucide/vue'
import DashboardLayout from '@/layouts/DashboardLayout.vue'

const groups = [
  {
    items: [
      { label: 'Overview', to: '/seller', icon: LayoutDashboard, exact: true },
      { label: 'Orders', to: '/seller/orders', icon: Package },
      { label: 'Returns', to: '/seller/returns', icon: RotateCcw },
    ],
  },
  {
    label: 'Catalog',
    items: [
      { label: 'Products', to: '/seller/products', icon: BarChart3 },
      { label: 'Inventory', to: '/seller/inventory', icon: Boxes },
    ],
  },
  { label: 'Store', items: [{ label: 'Store profile', to: '/seller/profile', icon: Store }] },
]
</script>

<template>
  <DashboardLayout title="Seller" :groups="groups">
    <template #header>
      <h1 class="text-base font-semibold">Seller dashboard</h1>
    </template>
  </DashboardLayout>
</template>
