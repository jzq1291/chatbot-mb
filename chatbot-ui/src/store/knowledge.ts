import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { KnowledgeBase } from '@/api/types'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeList = ref<KnowledgeBase[]>([])
  const loading = ref(false)
  const searchKeyword = ref('')
  const totalElements = ref(0)
  const currentPage = ref(1)
  const pageSize = 12

  const loadKnowledge = async (page: number, size: number) => {
    loading.value = true
    try {
      const response = await knowledgeApi.getKnowledgeList(page, size)
      knowledgeList.value = response.content
      totalElements.value = response.totalElements
      currentPage.value = page
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
      currentPage.value = page
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
      currentPage.value = page
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
      const totalPages = Math.ceil((totalElements.value + 1) / pageSize)
      return await loadKnowledge(totalPages, pageSize)
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
      return await loadKnowledge(currentPage.value, pageSize)
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
      if (knowledgeList.value.length === 1 && currentPage.value > 1) {
        return await loadKnowledge(currentPage.value - 1, pageSize)
      }
      return await loadKnowledge(currentPage.value, pageSize)
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
    currentPage,
    loadKnowledge,
    searchKnowledge,
    findByCategory,
    addKnowledge,
    updateKnowledge,
    deleteKnowledge,
    batchImportKnowledge
  }
}) 