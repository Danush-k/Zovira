import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ZPagination from '../ZPagination.vue'

describe('ZPagination', () => {
  it('renders nothing for a single page', () => {
    const wrapper = mount(ZPagination, { props: { page: 0, totalPages: 1 } })
    expect(wrapper.find('nav').exists()).toBe(false)
  })

  it('collapses long ranges with ellipses around the current page', () => {
    const wrapper = mount(ZPagination, { props: { page: 10, totalPages: 20 } })
    const labels = wrapper.findAll('button[aria-current], button:not([aria-label])').map((b) => b.text())
    expect(labels).toEqual(['1', '10', '11', '12', '20'])
    expect(wrapper.text()).toContain('...')
  })

  it('emits the zero-based page when a page is chosen', async () => {
    const wrapper = mount(ZPagination, { props: { page: 0, totalPages: 3 } })
    await wrapper.get('button[aria-label="Next page"]').trigger('click')
    expect(wrapper.emitted('update:page')?.[0]).toEqual([1])
  })
})
