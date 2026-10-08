<script setup>
import { FileClock, LayoutDashboard, MessageSquare, Package, RotateCcw, Settings, ShoppingBag, Store, Tags, TicketPercent, Users } from "@lucide/vue";
const groups = [
  { items: [{ label: "Overview", to: "/admin", icon: LayoutDashboard, exact: true }] },
  {
    label: "Commerce",
    items: [
      { label: "Orders", to: "/admin/orders", icon: ShoppingBag },
      { label: "Returns", to: "/admin/returns", icon: RotateCcw },
      { label: "Coupons", to: "/admin/coupons", icon: TicketPercent }
    ]
  },
  {
    label: "Catalog",
    items: [
      { label: "Products", to: "/admin/products", icon: Package },
      { label: "Categories", to: "/admin/categories", icon: Tags },
      { label: "Reviews", to: "/admin/reviews", icon: MessageSquare }
    ]
  },
  {
    label: "People",
    items: [
      { label: "Customers", to: "/admin/users", icon: Users },
      { label: "Sellers", to: "/admin/sellers", icon: Store }
    ]
  },
  {
    label: "Platform",
    items: [
      { label: "Settings", to: "/admin/settings", icon: Settings },
      { label: "Audit log", to: "/admin/audit-log", icon: FileClock }
    ]
  }
];
</script>

<template>
  <DashboardLayout title="Admin" :groups="groups">
    <template #header>
      <h1 class="text-base font-semibold">Admin console</h1>
    </template>
  </DashboardLayout>
</template>
