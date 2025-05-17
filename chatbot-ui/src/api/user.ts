import request from '@/utils/request'

export interface User {
  id?: number
  username: string
  email: string
  password?: string
  roles: string[]
}

export const userApi = {
  // 获取所有用户
  getAllUsers: () => {
    return request.get<User[]>('/ai/users')
  },

  // 创建用户
  createUser: (user: User) => {
    return request.post<User>('/ai/users', user)
  },

  // 更新用户
  updateUser: (user: User) => {
    if (!user.id) throw new Error('用户ID不能为空')
    return request.put<User>(`/ai/users/${user.id}`, user)
  },

  // 删除用户
  deleteUser: (id: number) => {
    return request.delete(`/ai/users/${id}`)
  }
} 