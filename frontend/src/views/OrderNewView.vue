<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getAvailableProducts } from '../api/products'
import { createOrder } from '../api/orders'
import { toApiError } from '../api/http'
import { useOrderCart } from '../composables/useOrderCart'
import type { Product } from '../types/product'
import type { OrderResponse } from '../types/order'

const MEMBER_ID_PATTERN = /^[0-9A-Za-z]{1,20}$/

const products = ref<Product[]>([])
const loadingProducts = ref(false)
const loadError = ref('')

const memberId = ref('')
const submitting = ref(false)
const submitError = ref('')
const orderResult = ref<OrderResponse | null>(null)

const cart = reactive(useOrderCart(products))

const memberIdValid = computed(() => MEMBER_ID_PATTERN.test(memberId.value))
const canSubmit = computed(
  () => memberIdValid.value && cart.hasValidSelection && !submitting.value,
)

async function loadProducts(): Promise<void> {
  loadingProducts.value = true
  loadError.value = ''
  try {
    products.value = await getAvailableProducts()
  } catch (error) {
    loadError.value = toApiError(error).message || '商品清單載入失敗'
  } finally {
    loadingProducts.value = false
  }
}

async function onSubmit(): Promise<void> {
  if (!canSubmit.value) {
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    const result = await createOrder({
      memberId: memberId.value,
      items: cart.toOrderItems(),
    })
    orderResult.value = result
    cart.reset()
    await loadProducts()
  } catch (error) {
    const apiError = toApiError(error)
    submitError.value = apiError.message || '訂單建立失敗，請稍後再試'
    if (apiError.code === 'INSUFFICIENT_STOCK') {
      await loadProducts()
    }
  } finally {
    submitting.value = false
  }
}

function startNewOrder(): void {
  orderResult.value = null
  submitError.value = ''
}

onMounted(loadProducts)
</script>

<template>
  <div class="card" v-if="orderResult">
    <h2>訂單建立成功</h2>
    <p class="alert alert-success">
      訂單編號：{{ orderResult.orderId }}，總金額：{{ orderResult.totalPrice }}
    </p>
    <ul>
      <li v-for="item in orderResult.items" :key="item.productId">
        {{ item.productName }} × {{ item.quantity }} = {{ item.itemPrice }}
      </li>
    </ul>
    <button type="button" @click="startNewOrder">建立下一筆訂單</button>
  </div>

  <div v-else class="card">
    <h2>建立訂單</h2>

    <p v-if="submitError" class="alert alert-error">{{ submitError }}</p>
    <p v-if="loadError" class="alert alert-error">{{ loadError }}</p>

    <div class="field">
      <label for="memberId">會員編號</label>
      <input id="memberId" v-model="memberId" type="text" maxlength="20" />
      <span v-if="memberId && !memberIdValid" class="error">需為 1~20 字的英數字</span>
    </div>

    <p v-if="loadingProducts">商品清單載入中...</p>

    <template v-else>
      <div v-for="product in products" :key="product.productId" class="product-row">
        <input
          type="checkbox"
          :checked="cart.isSelected(product.productId)"
          @change="cart.toggle(product.productId)"
        />
        <span>{{ product.productName }}（庫存 {{ product.quantity }}）</span>
        <span>單價 {{ product.price }}</span>
        <input
          type="text"
          inputmode="numeric"
          :disabled="!cart.isSelected(product.productId)"
          :value="cart.quantityInput(product.productId)"
          @input="cart.setQuantity(product.productId, ($event.target as HTMLInputElement).value)"
        />
        <span>
          <template v-if="cart.isSelected(product.productId)">
            <span v-if="cart.isQuantityValid(product.productId)">
              小計 {{ cart.subtotalOf(product.productId) }}
            </span>
            <span v-else class="error">數量無效</span>
          </template>
        </span>
      </div>

      <p v-if="products.length === 0" class="hint">目前無庫存商品可下單</p>

      <div class="total-row">
        <span>預估總額</span>
        <span>{{ cart.total }}</span>
      </div>
      <p class="hint">以上為試算結果，實際金額以系統為準</p>

      <button type="button" :disabled="!canSubmit" @click="onSubmit">
        {{ submitting ? '送出中...' : '送出訂單' }}
      </button>
    </template>
  </div>
</template>
