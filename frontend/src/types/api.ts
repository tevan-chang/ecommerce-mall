export type ErrorCode =
  | 'SUCCESS'
  | 'VALIDATION_ERROR'
  | 'PRODUCT_NOT_FOUND'
  | 'DUPLICATE_PRODUCT'
  | 'INSUFFICIENT_STOCK'
  | 'INTERNAL_ERROR'

export interface ApiResponse<T> {
  code: ErrorCode
  message: string
  data: T | null
}

export interface ApiError {
  code: ErrorCode
  message: string
}
