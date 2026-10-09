<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { createProduct } from '../api/products'
import { toApiError } from '../api/http'
import type { Product } from '../types/product'

const emit = defineEmits<{
  submitted: [product: Product]
  close: []
}>()

const PRODUCT_NAME_PATTERN = /^[^<>]{1,100}$/
const AMOUNT_PATTERN = /^(0|[1-9][0-9]*)$/

const form = reactive({
  productName: '',
  price: '',
  quantity: '',
})

const submitting = ref(false)
const errorMessage = ref('')

function isValidPrice(value: string): boolean {
  return AMOUNT_PATTERN.test(value) && Number(value) <= 99_999_999
}

function isValidQuantity(value: string): boolean {
  return AMOUNT_PATTERN.test(value) && Number(value) <= 1_000_000
}

const productNameValid = computed(() => PRODUCT_NAME_PATTERN.test(form.productName))
const priceValid = computed(() => isValidPrice(form.price))
const quantityValid = computed(() => isValidQuantity(form.quantity))

const canSubmit = computed(
  () => productNameValid.value && priceValid.value && quantityValid.value && !submitting.value,
)

async function onSubmit(): Promise<void> {
  if (!canSubmit.value) {
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const created = await createProduct({
      productName: form.productName,
      price: Number(form.price),
      quantity: Number(form.quantity),
    })
    emit('submitted', created)
  } catch (error) {
    errorMessage.value = toApiError(error).message || '新增失敗，請稍後再試'
  } finally {
    submitting.value = false
  }
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="modal-overlay" @click="emit('close')">
    <div class="modal" role="dialog" aria-modal="true" @click.stop>
      <div class="modal-header">
        <h3>新增商品</h3>
        <button type="button" class="modal-close" @click="emit('close')">&times;</button>
      </div>

      <p v-if="errorMessage" class="alert alert-error">{{ errorMessage }}</p>

      <form @submit.prevent="onSubmit">
        <div class="field">
          <label for="productName">商品名稱</label>
          <input id="productName" v-model="form.productName" type="text" maxlength="100" />
          <span v-if="form.productName && !productNameValid" class="error">
            需為 1~100 字，且不得包含 &lt; 或 &gt;
          </span>
        </div>

        <div class="field">
          <label for="price">價格</label>
          <input id="price" v-model="form.price" type="text" inputmode="numeric" />
          <span v-if="form.price && !priceValid" class="error">需為 0~99,999,999 的整數</span>
        </div>

        <div class="field">
          <label for="quantity">庫存數量</label>
          <input id="quantity" v-model="form.quantity" type="text" inputmode="numeric" />
          <span v-if="form.quantity && !quantityValid" class="error">需為 0~1,000,000 的整數</span>
        </div>

        <div class="modal-actions">
          <button type="button" class="button-secondary" @click="emit('close')">取消</button>
          <button type="submit" :disabled="!canSubmit">
            {{ submitting ? '送出中...' : '新增商品' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
