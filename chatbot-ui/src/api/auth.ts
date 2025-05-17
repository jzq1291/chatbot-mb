import request from '@/utils/request'
import type { LoginRequest, RegisterRequest, AuthResponse } from '@/api/types'

export const authApi = {
  login: (data: LoginRequest) => {
    return request.post<AuthResponse>('/ai/auth/login', data)
  },

  register: (data: RegisterRequest) => {
    return request.post<AuthResponse>('/ai/auth/register', data)
  },

  logout: () => {
    return request.post('/ai/auth/logout')
  }
} 