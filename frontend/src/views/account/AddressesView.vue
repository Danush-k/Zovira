<script setup lang="ts">
import { ref } from 'vue'
import { MapPin, Plus } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import AddressCard from '@/components/account/AddressCard.vue'
import AddressFormDialog from '@/components/account/AddressFormDialog.vue'
import { accountApi } from '@/services/account'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { Address } from '@/types/api'

const auth = useAuthStore()
const toast = useToastStore()
const { data: addresses, loading, error, execute: load } = useAsync(accountApi.addresses, { immediate: true })

const formOpen = ref(false)
const editing = ref<Address | null>(null)
const confirmDelete = ref<Address | null>(null)
const deleting = ref(false)

function add() {
  editing.value = null
  formOpen.value = true
}

function edit(address: Address) {
  editing.value = address
  formOpen.value = true
}

async function onSaved() {
  toast.success(editing.value ? 'Address updated' : 'Address added')
  await load()
}

async function makeDefault(address: Address) {
  try {
    await accountApi.makeDefaultAddress(address.id)
    await load()
  } catch (e) {
    toast.error("Couldn't update default address", errorMessage(e))
  }
}

async function remove() {
  if (!confirmDelete.value) return
  deleting.value = true
  try {
    await accountApi.deleteAddress(confirmDelete.value.id)
    confirmDelete.value = null
    toast.success('Address removed')
    await load()
  } catch (e) {
    toast.error("Couldn't remove address", errorMessage(e))
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div>
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Addresses</h1>
        <p class="mt-1 text-ink-500">Where should we deliver your orders?</p>
      </div>
      <ZButton v-if="addresses?.length" @click="add">
        <Plus class="size-4" />
        Add address
      </ZButton>
    </div>

    <div v-if="loading && !addresses" class="mt-6 grid gap-4 md:grid-cols-2">
      <div v-for="i in 2" :key="i" class="surface space-y-3 p-5">
        <div class="skeleton h-4 w-40" />
        <div class="skeleton h-3 w-full" />
        <div class="skeleton h-3 w-2/3" />
      </div>
    </div>
    <ZErrorState v-else-if="error" :error="error" @retry="load" />
    <div v-else-if="!addresses?.length" class="surface mt-6">
      <ZEmptyState :icon="MapPin" title="No saved addresses yet" description="Add an address to check out faster.">
        <ZButton @click="add"><Plus class="size-4" /> Add address</ZButton>
      </ZEmptyState>
    </div>
    <ul v-else class="mt-6 grid gap-4 md:grid-cols-2">
      <li v-for="address in addresses" :key="address.id" class="surface flex flex-col p-5">
        <AddressCard :address="address" class="flex-1" />
        <div class="mt-4 flex flex-wrap gap-1 border-t border-ink-100 pt-3">
          <ZButton variant="ghost" size="sm" @click="edit(address)">Edit</ZButton>
          <ZButton variant="ghost" size="sm" @click="confirmDelete = address">Remove</ZButton>
          <ZButton v-if="!address.isDefault" variant="ghost" size="sm" @click="makeDefault(address)">
            Set as default
          </ZButton>
        </div>
      </li>
    </ul>

    <AddressFormDialog
      v-model:open="formOpen"
      :address="editing"
      :default-name="auth.user?.fullName"
      :default-phone="auth.user?.phone"
      @saved="onSaved"
    />

    <ZDialog
      :open="!!confirmDelete"
      title="Remove this address?"
      description="You can add it again at any time."
      size="sm"
      @update:open="(v) => !v && (confirmDelete = null)"
    >
      <p v-if="confirmDelete" class="text-sm text-ink-600">
        {{ confirmDelete.fullName }}, {{ confirmDelete.line1 }}, {{ confirmDelete.city }}
      </p>
      <template #footer>
        <ZButton variant="ghost" @click="confirmDelete = null">Cancel</ZButton>
        <ZButton variant="danger" :loading="deleting" @click="remove">Remove</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
