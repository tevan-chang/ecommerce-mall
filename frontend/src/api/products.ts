import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { Product, ProductCreateRequest } from '../types/product'

export async function createProduct(request: ProductCreateRequest): Promise<Product> {
  const response = await http.post<ApiResponse<Product>>('/products', request)
  if (!response.data.data) {
    throw new Error('後端未回傳商品資料')
  }
  return response.data.data
}

export async function getAvailableProducts(): Promise<Product[]> {
  const response = await http.get<ApiResponse<Product[]>>('/products', {
    params: { inStock: true },
  })
  return response.data.data ?? []
}

export async function getAllProducts(): Promise<Product[]> {
  const response = await http.get<ApiResponse<Product[]>>('/products')
  return response.data.data ?? []
}
