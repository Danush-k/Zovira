<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import { adminApi } from "@/services/admin";
import { errorMessage } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useToastStore } from "@/stores/toast";
const route = useRoute();
const status = ref(typeof route.query.status === "string" ? route.query.status : "ALL");
const page = ref(0);
const toast = useToastStore();
const busy = ref(null);
const { data, loading, error, execute } = useAsync((s, p) => adminApi.reviews({ status: s, page: p, size: 20 }));
watch([status, page], () => void execute(status.value, page.value), { immediate: true });
watch(status, () => page.value = 0);
const tabs = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Awaiting review" },
  { value: "PUBLISHED", label: "Published" },
  { value: "REJECTED", label: "Rejected" }
];
async function moderate(id, next) {
  busy.value = id;
  try {
    await adminApi.moderateReview(id, next);
    await execute(status.value, page.value);
    toast.success(next === "PUBLISHED" ? "Review published" : "Review removed");
  } catch (e) {
    toast.error("Couldn't moderate the review", errorMessage(e));
  } finally {
    busy.value = null;
  }
}
</script>

<template>
  <div>
    <h1 class="text-xl font-semibold tracking-tight">Reviews</h1>
    <p class="mt-1 text-sm text-ink-500">Remove reviews that break the marketplace rules.</p>
    <ZTabs v-model="status" :tabs="tabs" label="Filter reviews" class="mt-5" />

    <ZErrorState v-if="error" :error="error" @retry="execute(status, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="skeleton h-28 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5"><ZEmptyState :icon="MessageSquare" title="Nothing to moderate" description="Reviews appear here as customers write them." compact /></div>
    <ul v-else class="mt-5 space-y-3">
      <li v-for="r in data.content" :key="r.id" class="surface flex flex-wrap gap-4 p-5">
        <img v-if="r.imageUrl" :src="r.imageUrl" alt="" class="size-14 rounded-lg bg-ink-50 object-contain p-1.5 mix-blend-multiply" />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-2">
            <ZRatingStars :value="r.rating" size="sm" />
            <RouterLink :to="`/p/${r.productSlug}`" class="text-sm font-medium hover:text-brand-800">{{ r.productTitle }}</RouterLink>
            <ZBadge v-if="r.verifiedPurchase" tone="success" size="sm">Verified purchase</ZBadge>
            <ZBadge :tone="r.status === 'PUBLISHED' ? 'success' : r.status === 'PENDING' ? 'warning' : 'danger'" size="sm">{{ r.status.toLowerCase() }}</ZBadge>
          </div>
          <p v-if="r.title" class="mt-2 text-sm font-semibold">{{ r.title }}</p>
          <p v-if="r.body" class="mt-1 text-sm text-ink-700">{{ r.body }}</p>
          <ul v-if="r.images.length" class="mt-2 flex gap-2">
            <li v-for="img in r.images" :key="img"><img :src="img" alt="" class="size-14 rounded-lg object-cover" /></li>
          </ul>
          <p class="mt-2 text-[13px] text-ink-500">{{ r.customerName }} · {{ formatDate(r.createdAt) }}<template v-if="r.helpfulCount"> · {{ r.helpfulCount }} found helpful</template></p>
        </div>
        <div class="flex gap-1 self-start">
          <ZButton v-if="r.status !== 'PUBLISHED'" size="sm" :loading="busy === r.id" @click="moderate(r.id, 'PUBLISHED')">Publish</ZButton>
          <ZButton v-if="r.status !== 'REJECTED'" size="sm" variant="ghost" :disabled="busy === r.id" @click="moderate(r.id, 'REJECTED')">Remove</ZButton>
        </div>
      </li>
    </ul>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />
  </div>
</template>
