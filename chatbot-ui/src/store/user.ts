import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User } from '@/api/user'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const userList = ref<User[]>([])
  const loading = ref(false)
  const totalElements = ref(0)

  const loadUsers = async (page = 0, size = 6) => {
    loading.value = true
    try {
      const response = await userApi.getAll(page, size)
      userList.value = response.content
      totalElements.value = response.totalElements
    } finally {
      loading.value = false
    }
  }

  const createUser = async (user: User) => {
    loading.value = true
    try {
      const response = await userApi.create(user)
      await loadUsers()
      return response
    } finally {
      loading.value = false
    }
  }

  const updateUser = async (user: User) => {
    loading.value = true
    try {
      const response = await userApi.update(user.id!, user)
      await loadUsers()
      return response
    } finally {
      loading.value = false
    }
  }

  const deleteUser = async (id: number) => {
    loading.value = true
    try {
      await userApi.delete(id)
      await loadUsers()
    } finally {
      loading.value = false
    }
  }

  return {
    userList,
    loading,
    totalElements,
    loadUsers,
    createUser,
    updateUser,
    deleteUser
  }
}) 