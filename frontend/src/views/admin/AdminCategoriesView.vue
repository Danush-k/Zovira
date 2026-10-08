<script setup>
import { reactive, ref } from "vue";
import { adminApi } from "@/services/admin";
import { errorMessage, fieldErrors } from "@/services/errors";
import { useAsync } from "@/composables/useAsync";
import { useToastStore } from "@/stores/toast";
const { data: tree, loading, error, execute } = useAsync(adminApi.categories, { immediate: true });
const toast = useToastStore();
const open = ref(false);
const editing = ref(null);
const errors = ref({});
const formError = ref(null);
const saving = ref(false);
const form = reactive({ name: "", parentId: null, description: "", icon: "", imageUrl: "", taxRate: 18, sortOrder: 0, active: true });
const icons = ["smartphone", "headphones", "laptop", "shirt", "footprints", "watch", "sparkles", "cooking-pot", "sofa", "dumbbell", "book-open"];
function add(parent) {
  editing.value = null;
  Object.assign(form, { name: "", parentId: parent?.id ?? null, description: "", icon: "", imageUrl: "", taxRate: 18, sortOrder: 0, active: true });
  errors.value = {};
  formError.value = null;
  open.value = true;
}
function edit(node, parent) {
  editing.value = node;
  Object.assign(form, {
    name: node.name,
    parentId: parent?.id ?? null,
    description: node.description ?? "",
    icon: node.icon ?? "",
    imageUrl: node.imageUrl ?? "",
    taxRate: 18,
    sortOrder: 0,
    active: true
  });
  errors.value = {};
  formError.value = null;
  open.value = true;
}
async function save() {
  errors.value = {};
  formError.value = null;
  saving.value = true;
  try {
    const payload = { ...form, taxRate: Number(form.taxRate) };
    const body = { ...payload, parentId: payload.parentId || null };
    if (editing.value) await adminApi.updateCategory(editing.value.id, body);
    else await adminApi.createCategory(body);
    open.value = false;
    await execute();
    toast.success(editing.value ? "Category updated" : "Category created");
  } catch (e) {
    errors.value = fieldErrors(e);
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="max-w-4xl">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div><h1 class="text-xl font-semibold tracking-tight">Categories</h1><p class="mt-1 text-sm text-ink-500">The shop-by-category structure, two levels deep.</p></div>
      <ZButton @click="add()"><Plus class="size-4" /> Add category</ZButton>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !tree" class="mt-5 space-y-2"><div v-for="i in 5" :key="i" class="skeleton h-14 rounded-lg" /></div>
    <ul v-else class="mt-5 space-y-3">
      <li v-for="top in tree ?? []" :key="top.id" class="surface overflow-hidden">
        <div class="flex items-center gap-3 p-4">
          <span class="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-700"><CategoryIcon :name="top.icon" class="size-5" /></span>
          <div class="min-w-0 flex-1">
            <p class="font-semibold">{{ top.name }}</p>
            <p class="text-[13px] text-ink-500">/{{ top.slug }}<template v-if="top.children.length"> · {{ top.children.length }} subcategories</template></p>
          </div>
          <ZButton variant="ghost" size="sm" @click="edit(top, null)">Edit</ZButton>
          <ZButton variant="ghost" size="sm" @click="add(top)">Add child</ZButton>
        </div>
        <ul v-if="top.children.length" class="divide-y divide-ink-100 border-t border-ink-100 bg-ink-50/50">
          <li v-for="child in top.children" :key="child.id" class="flex items-center gap-3 py-2.5 pr-4 pl-16">
            <div class="min-w-0 flex-1"><p class="text-sm font-medium">{{ child.name }}</p><p class="text-[13px] text-ink-500">/{{ child.slug }}</p></div>
            <ZButton variant="ghost" size="sm" @click="edit(child, top)">Edit</ZButton>
          </li>
        </ul>
      </li>
    </ul>

    <ZDialog v-model:open="open" :title="editing ? 'Edit category' : 'Add category'" size="md">
      <FormAlert v-if="formError" :message="formError" class="mb-4" />
      <form id="category-form" class="space-y-4" @submit.prevent="save">
        <ZInput v-model="form.name" label="Name" :error="errors.name" />
        <ZSelect v-model="form.parentId" label="Parent category" :options="[{ value: 0, label: 'None (top level)' }, ...(tree ?? []).map((t) => ({ value: t.id, label: t.name }))]" />
        <ZTextarea v-model="form.description" label="Description" :rows="2" :maxlength="500" optional />
        <div class="grid gap-4 sm:grid-cols-3">
          <ZSelect v-model="form.icon" label="Icon" :options="[{ value: '', label: 'None' }, ...icons.map((i) => ({ value: i, label: i }))]" />
          <ZInput v-model.number="form.taxRate" label="GST rate (%)" type="number" :error="errors.taxRate" />
          <ZInput v-model.number="form.sortOrder" label="Sort order" type="number" />
        </div>
        <ZInput v-model="form.imageUrl" label="Tile image URL" optional />
        <ZCheckbox v-model="form.active" label="Visible to shoppers" />
      </form>
      <template #footer>
        <ZButton variant="ghost" @click="open = false">Cancel</ZButton>
        <ZButton type="submit" form="category-form" :loading="saving">{{ editing ? 'Save' : 'Create' }}</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
