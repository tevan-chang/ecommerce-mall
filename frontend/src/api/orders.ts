import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { OrderCreateRequest, OrderResponse } from '../types/order'

export async function createOrder(request: OrderCreateRequest): Promise<OrderResponse> {
  const response = await http.post<ApiResponse<OrderResponse>>('/orders', request)
  if (!response.data.data) {
    throw new Error('後端未回傳訂單資料')
  }
  return response.data.data
}
