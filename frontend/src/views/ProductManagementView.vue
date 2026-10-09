<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAllProducts } from '../api/products'
import { toApiError } from '../api/http'
import ProductFormModal from '../components/ProductFormModal.vue'
import type { Product } from '../types/product'

const PLACEHOLDER_COLORS = [
  '#2563eb',
  '#7c3aed',
  '#db2777',
  '#059669',
  '#d97706',
  '#0891b2',
  '#dc2626',
  '#4f46e5',
]

const products = ref<Product[]>([])
const loading = ref(false)
const loadError = ref('')
const searchQuery = ref('')
const showModal = ref(false)
const successMessage = ref('')

const filteredProducts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) {
    return products.value
  }
  return products.value.filter(
    (p) =>
      p.productName.toLowerCase().includes(query) || p.productId.toLowerCase().includes(query),
  )
})

function placeholderColor(productId: string): string {
  let hash = 0
  for (let i = 0; i < productId.length; i += 1) {
    hash = (hash + productId.charCodeAt(i)) % PLACEHOLDER_COLORS.length
  }
  return PLACEHOLDER_COLORS[hash] ?? PLACEHOLDER_COLORS[0]
}

async function loadProducts(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    products.value = await getAllProducts()
  } catch (error) {
    loadError.value = toApiError(error).message || '商品清單載入失敗'
  } finally {
    loading.value = false
  }
}

async function onCreated(product: Product): Promise<void> {
  showModal.value = false
  await loadProducts()
  successMessage.value = `商品 ${product.productId} 已新增`
}

onMounted(loadProducts)
</script>

<template>
  <div>
    <div class="page-header-row">
      <h2>商品管理</h2>
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="搜尋商品名稱或編號"
      />
      <button type="button" @click="showModal = true">+ 新增商品</button>
    </div>

    <p v-if="successMessage" class="alert alert-success">{{ successMessage }}</p>
    <p v-if="loadError" class="alert alert-error">{{ loadError }}</p>

    <p v-if="loading" class="hint">商品清單載入中...</p>

    <template v-else>
      <p v-if="products.length === 0" class="hint">尚無商品</p>
      <p v-else-if="filteredProducts.length === 0" class="hint">沒有符合的商品</p>

      <div v-else class="product-grid">
        <div v-for="product in filteredProducts" :key="product.productId" class="product-card">
          <div
            class="product-card-image"
            :style="{ background: placeholderColor(product.productId) }"
          >
            {{ product.productName.charAt(0) }}
          </div>
          <div class="product-card-body">
            <span class="product-card-name">{{ product.productName }}</span>
            <span class="product-card-id">{{ product.productId }}</span>
            <span class="product-card-price">NT$ {{ product.price }}</span>
            <span class="product-card-stock">庫存：{{ product.quantity }}</span>
            <span class="badge" :class="product.quantity > 0 ? 'badge-instock' : 'badge-outofstock'">
              {{ product.quantity > 0 ? '有庫存' : '缺貨' }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <ProductFormModal
      v-if="showModal"
      @submitted="onCreated"
      @close="showModal = false"
    />
  </div>
</template>
