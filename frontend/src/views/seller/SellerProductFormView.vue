<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ImagePlus, Plus, Trash2 } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZTextarea from '@/components/ui/ZTextarea.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import ZCheckbox from '@/components/ui/ZCheckbox.vue'
import ZSpinner from '@/components/ui/ZSpinner.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import { sellerApi } from '@/services/seller'
import { catalogApi } from '@/services/catalog'
import { http } from '@/services/http'
import { errorMessage, fieldErrors } from '@/services/errors'
import { useCatalogStore } from '@/stores/catalog'
import { useToastStore } from '@/stores/toast'
import type { SellerProductInput, VariantInput } from '@/types/seller'
import type { Brand } from '@/types/catalog'

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()
const toast = useToastStore()
const productId = computed(() => (route.params.id === 'new' ? null : Number(route.params.id)))

const form = reactive<SellerProductInput>({
  title: '',
  categoryId: null,
  brandId: null,
  newBrandName: '',
  shortDescription: '',
  description: '',
  highlights: [],
  tags: '',
  warranty: '',
  returnable: true,
  returnWindowDays: 7,
  codAvailable: true,
  modelUrl: '',
  arModelUrl: '',
  attributes: [],
  variants: [{ options: {}, price: 0, mrp: 0, stock: 0, isDefault: true, active: true, name: 'Standard' }],
  images: [],
  specifications: [],
  publish: false,
})

const brands = ref<Brand[]>([])
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const saving = ref(false)
const uploading = ref(false)
const loading = ref(false)
const highlightsText = ref('')

const leafCategories = computed(() =>
  catalog.flat.filter((c) => !c.children.length).map((c) => ({ value: c.id, label: c.parent ? `${c.parent.name} › ${c.name}` : c.name })),
)
const brandOptions = computed(() => [{ value: 0, label: 'No brand / add new' }, ...brands.value.map((b) => ({ value: b.id, label: b.name }))])

onMounted(async () => {
  await catalog.loadCategories().catch(() => undefined)
  brands.value = await catalogApi.brands().catch(() => [])
  if (productId.value) {
    loading.value = true
    try {
      const page = await sellerApi.products({ size: 100 })
      const found = page.content.find((p) => p.id === productId.value)
      if (!found) throw new Error('Not found')
      const detail = await catalogApi.product(found.slug)
      form.title = detail.title
      form.categoryId = detail.category.id
      form.brandId = detail.brand?.id ?? null
      form.shortDescription = detail.shortDescription ?? ''
      form.description = detail.description ?? ''
      form.highlights = detail.highlights
      highlightsText.value = detail.highlights.join('\n')
      form.tags = detail.tags.join(',')
      form.warranty = detail.warranty ?? ''
      form.returnable = detail.returnable
      form.returnWindowDays = detail.returnWindowDays || 7
      form.codAvailable = detail.codAvailable
      form.modelUrl = detail.model?.modelUrl ?? ''
      form.arModelUrl = detail.model?.arModelUrl ?? ''
      form.attributes = detail.attributes.map((a) => ({ name: a.name, options: a.options }))
      form.images = detail.images.map((i) => ({ url: i.url, altText: i.altText ?? undefined }))
      form.specifications = detail.specifications.flatMap((g) => g.items.map((i) => ({ group: g.group, name: i.name, value: i.value })))
      const inventory = await sellerApi.inventory()
      form.variants = detail.variants.map<VariantInput>((v) => ({
        id: v.id,
        sku: v.sku,
        name: v.name,
        options: v.options,
        price: v.price,
        mrp: v.mrp,
        stock: inventory.find((r) => r.variantId === v.id)?.available ?? 0,
        isDefault: v.isDefault,
        active: true,
      }))
      form.publish = detail.status === 'ACTIVE'
    } catch (e) {
      formError.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }
})

function addVariant() {
  form.variants.push({ options: {}, price: 0, mrp: 0, stock: 0, isDefault: false, active: true, name: '' })
}

function removeVariant(index: number) {
  form.variants.splice(index, 1)
  if (!form.variants.some((v) => v.isDefault) && form.variants[0]) form.variants[0].isDefault = true
}

function setDefault(index: number) {
  form.variants.forEach((v, i) => (v.isDefault = i === index))
}

function addSpec() {
  form.specifications!.push({ group: 'General', name: '', value: '' })
}

async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return
  uploading.value = true
  try {
    const body = new FormData()
    Array.from(input.files).slice(0, 12).forEach((f) => body.append('files', f))
    const { data } = await http.post<{ url: string }[]>('/seller/products/images', body)
    form.images = [...(form.images ?? []), ...data.map((d) => ({ url: d.url }))]
  } catch (e) {
    toast.error("Couldn't upload images", errorMessage(e))
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function save(publish: boolean) {
  errors.value = {}
  formError.value = null
  form.publish = publish
  form.highlights = highlightsText.value.split('\n').map((s) => s.trim()).filter(Boolean).slice(0, 10)
  if (!form.categoryId) errors.value.categoryId = 'Choose a category'
  if (form.title.trim().length < 5) errors.value.title = 'Enter a product title of at least 5 characters'
  if (!form.variants.length) formError.value = 'Add at least one variant'
  if (Object.keys(errors.value).length || formError.value) return

  saving.value = true
  try {
    const payload: SellerProductInput = { ...form, brandId: form.brandId || null, specifications: form.specifications?.filter((s) => s.name && s.value) }
    if (productId.value) await sellerApi.updateProduct(productId.value, payload)
    else await sellerApi.createProduct(payload)
    toast.success(publish ? 'Product published' : 'Draft saved')
    await router.push('/seller/products')
  } catch (e) {
    errors.value = fieldErrors(e)
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="max-w-4xl">
    <RouterLink to="/seller/products" class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-600 hover:text-ink-900">
      <ArrowLeft class="size-4" /> Products
    </RouterLink>
    <h1 class="mt-3 text-xl font-semibold tracking-tight">{{ productId ? 'Edit product' : 'Add a product' }}</h1>

    <div v-if="loading" class="mt-6 space-y-4"><div v-for="i in 3" :key="i" class="skeleton h-40 rounded-xl" /></div>
    <form v-else class="mt-6 space-y-6" novalidate @submit.prevent="save(true)">
      <FormAlert v-if="formError" :message="formError" />

      <section class="surface space-y-4 p-5">
        <h2 class="font-semibold">Basics</h2>
        <ZInput v-model="form.title" label="Product title" :error="errors.title" hint="Include the brand, model and key attribute" />
        <div class="grid gap-4 sm:grid-cols-2">
          <ZSelect v-model="form.categoryId" label="Category" placeholder="Choose a category" :options="leafCategories" :error="errors.categoryId" />
          <ZSelect v-model="form.brandId" label="Brand" :options="brandOptions" />
        </div>
        <ZInput v-if="!form.brandId" v-model="form.newBrandName" label="New brand name" optional hint="Leave empty for unbranded products" />
        <ZTextarea v-model="form.shortDescription" label="Short description" :maxlength="500" :rows="2" hint="One or two lines shown near the title" />
        <ZTextarea v-model="form.description" label="Full description" :maxlength="20000" :rows="6" />
        <ZTextarea v-model="highlightsText" label="Highlights" :rows="4" optional hint="One per line, up to 10" />
      </section>

      <section class="surface space-y-4 p-5">
        <div class="flex items-center justify-between">
          <h2 class="font-semibold">Images</h2>
          <label class="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-ink-200 px-3 py-1.5 text-sm font-semibold hover:bg-ink-50">
            <ZSpinner v-if="uploading" class="size-4" /><ImagePlus v-else class="size-4" />
            Upload
            <input type="file" accept="image/jpeg,image/png,image/webp,image/avif" multiple class="sr-only" @change="upload" />
          </label>
        </div>
        <p class="text-[13px] text-ink-500">JPEG, PNG, WebP or AVIF up to 5 MB each. The first image is the main one.</p>
        <ul v-if="form.images?.length" class="grid grid-cols-3 gap-3 sm:grid-cols-6">
          <li v-for="(img, i) in form.images" :key="img.url" class="relative">
            <img :src="img.url" alt="" class="aspect-square w-full rounded-lg border border-ink-150 bg-ink-50 object-contain p-1.5" />
            <button type="button" class="absolute -top-2 -right-2 grid size-7 place-items-center rounded-full bg-white shadow-raised hover:text-danger" :aria-label="`Remove image ${i + 1}`" @click="form.images!.splice(i, 1)">
              <Trash2 class="size-3.5" />
            </button>
          </li>
        </ul>
      </section>

      <section class="surface space-y-4 p-5">
        <div class="flex items-center justify-between">
          <div><h2 class="font-semibold">Variants and pricing</h2><p class="text-[13px] text-ink-500">Each row is a buyable option with its own price and stock.</p></div>
          <ZButton type="button" variant="outline" size="sm" @click="addVariant"><Plus class="size-4" /> Add variant</ZButton>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full min-w-[640px] text-sm">
            <thead class="text-left text-xs tracking-wide text-ink-500 uppercase">
              <tr><th class="pb-2 font-semibold">Name</th><th class="pb-2 font-semibold">Price</th><th class="pb-2 font-semibold">MRP</th><th class="pb-2 font-semibold">Stock</th><th class="pb-2 font-semibold">Default</th><th /></tr>
            </thead>
            <tbody>
              <tr v-for="(v, i) in form.variants" :key="i" class="border-t border-ink-100">
                <td class="py-2 pr-2"><input v-model="v.name" placeholder="e.g. Blue, 128 GB" class="field-control h-9" :aria-label="`Variant ${i + 1} name`" /></td>
                <td class="py-2 pr-2"><input v-model.number="v.price" type="number" min="1" class="field-control h-9 w-28" :aria-label="`Variant ${i + 1} price`" /></td>
                <td class="py-2 pr-2"><input v-model.number="v.mrp" type="number" min="1" class="field-control h-9 w-28" :aria-label="`Variant ${i + 1} MRP`" /></td>
                <td class="py-2 pr-2"><input v-model.number="v.stock" type="number" min="0" class="field-control h-9 w-24" :aria-label="`Variant ${i + 1} stock`" /></td>
                <td class="py-2 pr-2"><input type="radio" name="default-variant" :checked="v.isDefault" class="accent-brand-700" :aria-label="`Make variant ${i + 1} the default`" @change="setDefault(i)" /></td>
                <td class="py-2 text-right">
                  <button v-if="form.variants.length > 1" type="button" class="text-ink-400 hover:text-danger" :aria-label="`Remove variant ${i + 1}`" @click="removeVariant(i)"><Trash2 class="size-4" /></button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="surface space-y-4 p-5">
        <div class="flex items-center justify-between">
          <h2 class="font-semibold">Specifications</h2>
          <ZButton type="button" variant="outline" size="sm" @click="addSpec"><Plus class="size-4" /> Add row</ZButton>
        </div>
        <div v-for="(s, i) in form.specifications" :key="i" class="grid gap-2 sm:grid-cols-[160px_1fr_1fr_auto]">
          <input v-model="s.group" placeholder="Group" class="field-control h-9" :aria-label="`Spec ${i + 1} group`" />
          <input v-model="s.name" placeholder="Name" class="field-control h-9" :aria-label="`Spec ${i + 1} name`" />
          <input v-model="s.value" placeholder="Value" class="field-control h-9" :aria-label="`Spec ${i + 1} value`" />
          <button type="button" class="px-2 text-ink-400 hover:text-danger" :aria-label="`Remove spec ${i + 1}`" @click="form.specifications!.splice(i, 1)"><Trash2 class="size-4" /></button>
        </div>
      </section>

      <section class="surface space-y-4 p-5">
        <h2 class="font-semibold">Policies and extras</h2>
        <div class="grid gap-4 sm:grid-cols-2">
          <ZInput v-model="form.warranty" label="Warranty" optional placeholder="1 year manufacturer warranty" />
          <ZInput v-model="form.tags" label="Search keywords" optional hint="Comma separated" />
          <ZInput v-model="form.modelUrl" label="3D model URL (.glb)" optional hint="Shoppers can view and place it in AR" />
          <ZInput v-model="form.arModelUrl" label="iOS AR model URL (.usdz)" optional />
        </div>
        <div class="flex flex-wrap gap-6">
          <ZCheckbox v-model="form.returnable" label="Accept returns" />
          <ZInput v-if="form.returnable" v-model.number="form.returnWindowDays" label="Return window (days)" type="number" class="w-40" />
          <ZCheckbox v-model="form.codAvailable" label="Allow cash on delivery" />
        </div>
      </section>

      <div class="flex flex-wrap justify-end gap-3 pb-4">
        <ZButton type="button" variant="outline" :loading="saving" @click="save(false)">Save as draft</ZButton>
        <ZButton type="submit" :loading="saving">{{ productId ? 'Save and publish' : 'Publish product' }}</ZButton>
      </div>
    </form>
  </div>
</template>
