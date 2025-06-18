import request from '@/utils/request'
import type { PageResponse } from './types'

export interface User {
  id?: number
  username: string
  email: string
  password?: string
  roles: string[]
}

export const userApi = {
  getAll: (page: number, size: number) => {
    return request.get<PageResponse<User>>('/ai/users', {
      params: { page, size }
    })
  },

  getById: (id: number) => {
    return request.get<User>(`/ai/users/${id}`)
  },

  create: (user: User) => {
    return request.post<User>('/ai/users', user)
  },

  update: (id: number, user: User) => {
    return request.put<User>(`/ai/users/${id}`, user)
  },

  delete: (id: number) => {
    return request.delete(`/ai/users/${id}`)
  }
} 