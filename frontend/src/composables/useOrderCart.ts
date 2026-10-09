import { computed, reactive, type Ref } from 'vue'
import type { Product } from '../types/product'
import type { OrderItemRequest } from '../types/order'

interface CartEntry {
  selected: boolean
  quantityInput: string
}

const INTEGER_PATTERN = /^[1-9][0-9]*$/

export function useOrderCart(products: Ref<Product[]>) {
  const entries = reactive<Record<string, CartEntry>>({})

  function entryFor(productId: string): CartEntry {
    if (!entries[productId]) {
      entries[productId] = { selected: false, quantityInput: '' }
    }
    return entries[productId]
  }

  function isSelected(productId: string): boolean {
    return entries[productId]?.selected ?? false
  }

  function quantityInput(productId: string): string {
    return entries[productId]?.quantityInput ?? ''
  }

  function toggle(productId: string): void {
    const entry = entryFor(productId)
    entry.selected = !entry.selected
    if (!entry.selected) {
      entry.quantityInput = ''
    } else if (entry.quantityInput === '') {
      entry.quantityInput = '1'
    }
  }

  function setQuantity(productId: string, value: string): void {
    entryFor(productId).quantityInput = value
  }

  function stockOf(productId: string): number {
    return products.value.find((p) => p.productId === productId)?.quantity ?? 0
  }

  function quantityOf(productId: string): number | null {
    const raw = quantityInput(productId)
    if (!INTEGER_PATTERN.test(raw)) {
      return null
    }
    const value = Number(raw)
    return value >= 1 && value <= stockOf(productId) ? value : null
  }

  function isQuantityValid(productId: string): boolean {
    return quantityOf(productId) !== null
  }

  function subtotalOf(productId: string): number | null {
    const quantity = quantityOf(productId)
    if (quantity === null) {
      return null
    }
    const price = products.value.find((p) => p.productId === productId)?.price ?? 0
    return price * quantity
  }

  const selectedProductIds = computed(() => {
    const availableIds = new Set(products.value.map((p) => p.productId))
    return Object.keys(entries).filter((id) => entries[id]?.selected && availableIds.has(id))
  })

  const hasValidSelection = computed(
    () => selectedProductIds.value.length > 0 && selectedProductIds.value.every(isQuantityValid),
  )

  const total = computed(() =>
    selectedProductIds.value.reduce((sum, id) => sum + (subtotalOf(id) ?? 0), 0),
  )

  function toOrderItems(): OrderItemRequest[] {
    return selectedProductIds.value.map((productId) => ({
      productId,
      quantity: quantityOf(productId) as number,
    }))
  }

  function reset(): void {
    for (const key of Object.keys(entries)) {
      delete entries[key]
    }
  }

  return {
    isSelected,
    quantityInput,
    toggle,
    setQuantity,
    isQuantityValid,
    subtotalOf,
    selectedProductIds,
    hasValidSelection,
    total,
    toOrderItems,
    reset,
  }
}
