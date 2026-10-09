export interface Product {
  productId: string
  productName: string
  price: number
  quantity: number
}

export interface ProductCreateRequest {
  productName: string
  price: number
  quantity: number
}
