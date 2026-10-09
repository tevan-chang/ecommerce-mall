export interface OrderItemRequest {
  productId: string
  quantity: number
}

export interface OrderCreateRequest {
  memberId: string
  items: OrderItemRequest[]
}

export interface OrderItemResponse {
  productId: string
  productName: string
  quantity: number
  standPrice: number
  itemPrice: number
}

export interface OrderResponse {
  orderId: string
  memberId: string
  totalPrice: number
  payStatus: number
  items: OrderItemResponse[]
}
