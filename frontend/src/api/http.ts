import axios, { AxiosError } from 'axios'
import type { ApiError, ApiResponse } from '../types/api'

export const http = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

export function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiResponse<unknown>>
    const body = axiosError.response?.data
    if (body?.code && body?.message !== undefined) {
      return { code: body.code, message: body.message }
    }
  }
  return { code: 'INTERNAL_ERROR', message: '網路異常，請稍後再試' }
}
