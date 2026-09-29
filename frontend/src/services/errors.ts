import { isAxiosError } from 'axios'

export interface FieldViolation {
  field: string
  message: string
}

/** Normalized representation of an RFC 9457 problem response from the Zovira API. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldViolation[]
  readonly traceId?: string

  constructor(status: number, code: string, message: string, fieldErrors: FieldViolation[] = [], traceId?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.traceId = traceId
  }
}

const GENERIC = 'Something went wrong. Please try again.'

interface ProblemBody {
  detail?: string
  code?: string
  errors?: FieldViolation[]
  traceId?: string
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (isAxiosError(error)) {
    if (!error.response) {
      return new ApiError(0, 'NETWORK_ERROR', 'Unable to reach Zovira. Check your connection and try again.')
    }
    const body = (error.response.data ?? {}) as ProblemBody
    return new ApiError(
      error.response.status,
      body.code ?? 'HTTP_' + error.response.status,
      body.detail ?? GENERIC,
      body.errors ?? [],
      body.traceId,
    )
  }
  if (error instanceof Error) return new ApiError(0, 'CLIENT_ERROR', error.message)
  return new ApiError(0, 'UNKNOWN', GENERIC)
}

export function errorMessage(error: unknown, fallback = GENERIC): string {
  const apiError = toApiError(error)
  return apiError.message || fallback
}

export function isNetworkError(error: unknown): boolean {
  if (!error) return false
  return toApiError(error).code === 'NETWORK_ERROR'
}

export function isStatus(error: unknown, status: number): boolean {
  return toApiError(error).status === status
}

/** Maps server-side field violations onto a form's field names. */
export function fieldErrors(error: unknown): Record<string, string> {
  const map: Record<string, string> = {}
  for (const v of toApiError(error).fieldErrors) {
    if (!map[v.field]) map[v.field] = v.message
  }
  return map
}
