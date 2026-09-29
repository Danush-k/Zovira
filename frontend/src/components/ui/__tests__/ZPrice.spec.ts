import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ZPrice from '../ZPrice.vue'

describe('ZPrice', () => {
  it('shows price, struck MRP and discount when marked down', () => {
    const wrapper = mount(ZPrice, { props: { price: 749, mrp: 999 } })
    expect(wrapper.text()).toContain('₹749')
    expect(wrapper.text()).toContain('₹999')
    expect(wrapper.text()).toContain('25% off')
  })

  it('hides MRP and discount when there is no markdown', () => {
    const wrapper = mount(ZPrice, { props: { price: 999, mrp: 999 } })
    expect(wrapper.text()).not.toContain('off')
    expect(wrapper.find('.line-through').exists()).toBe(false)
  })
})
