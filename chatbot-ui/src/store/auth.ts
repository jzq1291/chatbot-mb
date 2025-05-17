import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { authApi } from '@/api/auth'
import { useRouter } from 'vue-router'
import type { LoginRequest, RegisterRequest, AuthResponse, UserRole } from '@/api/types'
import { hasRole, hasAnyRole } from '@/api/types'
import { useChatStore } from '@/store/chat'

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()
  const chatStore = useChatStore()
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const roles = ref<UserRole[]>(JSON.parse(localStorage.getItem('roles') || '[]'))
  const loading = ref(false)

  const setToken = (newToken: string | null) => {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)
      axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
    } else {
      localStorage.removeItem('token')
      delete axios.defaults.headers.common['Authorization']
    }
  }

  const setUsername = (newUsername: string | null) => {
    username.value = newUsername
    if (newUsername) {
      localStorage.setItem('username', newUsername)
    } else {
      localStorage.removeItem('username')
    }
  }

  const setRoles = (newRoles: UserRole[]) => {
    roles.value = newRoles
    localStorage.setItem('roles', JSON.stringify(newRoles))
  }

  const clearToken = () => {
    setToken(null)
    setUsername(null)
    setRoles([])
  }

  // 初始化时设置 axios 默认请求头
  if (token.value) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
  }

  const login = async (username: string, password: string) => {
    loading.value = true
    try {
      const response = await authApi.login({ username, password })
      const { token: newToken, username: newUsername, roles: newRoles } = response
      setToken(newToken)
      setUsername(newUsername)
      setRoles(newRoles as UserRole[])
      await router.push('/chat')
      return response
    } catch (error) {
      console.error('Login error:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const register = async (data: RegisterRequest) => {
    loading.value = true
    try {
      const response = await authApi.register(data)
      const { token: newToken, username: newUsername, roles: newRoles } = response
      setToken(newToken)
      setUsername(newUsername)
      setRoles(newRoles as UserRole[])
      await router.push('/chat')
      return response
    } finally {
      loading.value = false
    }
  }

  const logout = async () => {
    loading.value = true
    try {
      // 在清除 token 之前调用登出接口
      if (token.value) {
        await authApi.logout()
      }
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      // 清除所有状态
      clearToken()
      chatStore.resetState()
      // 清除 token 后立即跳转到登录页
      await router.push('/login')
      loading.value = false
    }
  }

  const checkRole = (role: UserRole): boolean => {
    return hasRole(roles.value, role)
  }

  const checkAnyRole = (requiredRoles: UserRole[]): boolean => {
    return hasAnyRole(roles.value, requiredRoles)
  }

  return {
    token,
    username,
    roles,
    loading,
    login,
    register,
    logout,
    checkRole,
    checkAnyRole
  }
}) 