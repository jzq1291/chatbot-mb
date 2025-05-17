import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userApi, type User } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const userList = ref<User[]>([])
  const loading = ref(false)

  const loadUsers = async () => {
    loading.value = true
    try {
      const users = await userApi.getAllUsers()
      userList.value = users
    } catch (error) {
      console.error('加载用户列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  const createUser = async (user: User) => {
    loading.value = true
    try {
      await userApi.createUser(user)
      await loadUsers()
    } finally {
      loading.value = false
    }
  }

  const updateUser = async (user: User) => {
    loading.value = true
    try {
      await userApi.updateUser(user)
      await loadUsers()
    } finally {
      loading.value = false
    }
  }

  const deleteUser = async (id: number) => {
    loading.value = true
    try {
      await userApi.deleteUser(id)
      await loadUsers()
    } finally {
      loading.value = false
    }
  }

  return {
    userList,
    loading,
    loadUsers,
    createUser,
    updateUser,
    deleteUser
  }
}) 