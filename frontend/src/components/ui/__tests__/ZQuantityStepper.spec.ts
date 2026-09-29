import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ZQuantityStepper from '../ZQuantityStepper.vue'

describe('ZQuantityStepper', () => {
  it('increments and emits the new value', async () => {
    const wrapper = mount(ZQuantityStepper, { props: { modelValue: 2, max: 5 } })
    await wrapper.get('button[aria-label="Increase quantity"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([3])
  })

  it('disables decrement at the minimum and increment at the maximum', () => {
    const atMin = mount(ZQuantityStepper, { props: { modelValue: 1, min: 1, max: 5 } })
    expect(atMin.get('button[aria-label="Decrease quantity"]').attributes('disabled')).toBeDefined()
    const atMax = mount(ZQuantityStepper, { props: { modelValue: 5, min: 1, max: 5 } })
    expect(atMax.get('button[aria-label="Increase quantity"]').attributes('disabled')).toBeDefined()
  })
})
