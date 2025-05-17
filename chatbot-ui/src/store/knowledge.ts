import { defineStore } from 'pinia'
import { ref } from 'vue'
import { knowledgeApi, type KnowledgeBase } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeList = ref<KnowledgeBase[]>([])
  const loading = ref(false)
  const searchKeyword = ref('')

  const loadKnowledge = async () => {
    loading.value = true
    try {
      const data = await knowledgeApi.searchKnowledge('')
      knowledgeList.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('加载知识列表失败:', error)
      knowledgeList.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  const searchKnowledge = async (keyword: string) => {
    loading.value = true
    try {
      searchKeyword.value = keyword
      const data = await knowledgeApi.searchKnowledge(keyword)
      knowledgeList.value = Array.isArray(data) ? data : []
    } catch (error) {
      console.error('搜索失败:', error)
      knowledgeList.value = []
      throw error
    } finally {
      loading.value = false
    }
  }

  const addKnowledge = async (knowledge: KnowledgeBase) => {
    loading.value = true
    try {
      await knowledgeApi.addKnowledge(knowledge)
      await loadKnowledge()
    } finally {
      loading.value = false
    }
  }

  const updateKnowledge = async (knowledge: KnowledgeBase) => {
    loading.value = true
    try {
      await knowledgeApi.updateKnowledge(knowledge)
      await loadKnowledge()
    } finally {
      loading.value = false
    }
  }

  const deleteKnowledge = async (id: number) => {
    loading.value = true
    try {
      await knowledgeApi.deleteKnowledge(id)
      await loadKnowledge()
    } finally {
      loading.value = false
    }
  }

  return {
    knowledgeList,
    loading,
    searchKeyword,
    loadKnowledge,
    searchKnowledge,
    addKnowledge,
    updateKnowledge,
    deleteKnowledge
  }
}) 