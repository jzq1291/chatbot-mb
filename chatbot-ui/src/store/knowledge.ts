import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { KnowledgeBase } from '@/api/types'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeList = ref<KnowledgeBase[]>([])
  const loading = ref(false)
  const searchKeyword = ref('')
  const totalElements = ref(0)
  const pageSize = 12

  const loadKnowledge = async (page: number, size: number) => {
    loading.value = true
    try {
      const response = await knowledgeApi.getKnowledgeList(page, size)
      knowledgeList.value = response.content
      totalElements.value = response.totalElements
      return {
        ...response,
        currentPage: page
      }
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const searchKnowledge = async (keyword: string, page: number, size: number) => {
    loading.value = true
    try {
      const response = await knowledgeApi.searchKnowledge(keyword, page, size)
      knowledgeList.value = response.content
      totalElements.value = response.totalElements
      return {
        ...response,
        currentPage: page
      }
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const findByCategory = async (category: string, page: number, size: number) => {
    loading.value = true
    try {
      const response = await knowledgeApi.findByCategory(category, page, size)
      knowledgeList.value = response.content
      totalElements.value = response.totalElements
      return {
        ...response,
        currentPage: page
      }
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const addKnowledge = async (knowledge: KnowledgeBase) => {
    loading.value = true
    try {
      await knowledgeApi.addKnowledge(knowledge)
      return await loadKnowledge(1, pageSize)
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const updateKnowledge = async (knowledge: KnowledgeBase) => {
    loading.value = true
    try {
      await knowledgeApi.updateKnowledge(knowledge)
      const currentPage = Math.ceil((knowledgeList.value.findIndex(item => item.id === knowledge.id) + 1) / pageSize)
      return await loadKnowledge(currentPage, pageSize)
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const deleteKnowledge = async (id: number) => {
    loading.value = true
    try {
      await knowledgeApi.deleteKnowledge(id)
      const newTotalElements = totalElements.value - 1
      const totalPages = Math.ceil(newTotalElements / pageSize)
      const currentPage = Math.min(
        Math.ceil(knowledgeList.value.findIndex(item => item.id === id) / pageSize),
        totalPages
      )
      const targetPage = currentPage === 0 ? 1 : currentPage
      return await loadKnowledge(targetPage, pageSize)
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  const batchImportKnowledge = async (knowledgeList: KnowledgeBase[]) => {
    loading.value = true
    try {
      await knowledgeApi.batchImportKnowledge(knowledgeList)
      return await loadKnowledge(1, pageSize)
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  return {
    knowledgeList,
    loading,
    searchKeyword,
    totalElements,
    loadKnowledge,
    searchKnowledge,
    findByCategory,
    addKnowledge,
    updateKnowledge,
    deleteKnowledge,
    batchImportKnowledge
  }
}) 