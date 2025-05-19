import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { KnowledgeBase } from '@/api/knowledge'
import { knowledgeApi } from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeList = ref<KnowledgeBase[]>([])
  const loading = ref(false)
  const searchKeyword = ref('')
  const totalElements = ref(0)

  const loadKnowledge = async (page = 1, size = 6) => {
    loading.value = true
    try {
      const response = await knowledgeApi.search("", page, size)
      knowledgeList.value = response.content
      totalElements.value = response.totalElements
    } finally {
      loading.value = false
    }
  }

  const searchKnowledge = async (keyword: string, page = 1, size = 6) => {
    loading.value = true
    try {
      const response = await knowledgeApi.search(keyword, page, size)
      knowledgeList.value = response.content
      totalElements.value = response.totalElements
    } finally {
      loading.value = false
    }
  }

  const addKnowledge = async (knowledge: KnowledgeBase) => {
    loading.value = true
    try {
      const response = await knowledgeApi.add(knowledge)
      await loadKnowledge()
      return response
    } finally {
      loading.value = false
    }
  }

  const updateKnowledge = async (knowledge: KnowledgeBase) => {
    loading.value = true
    try {
      const response = await knowledgeApi.update(knowledge.id!, knowledge)
      await loadKnowledge()
      return response
    } finally {
      loading.value = false
    }
  }

  const deleteKnowledge = async (id: number) => {
    loading.value = true
    try {
      await knowledgeApi.delete(id)
      await loadKnowledge()
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
    addKnowledge,
    updateKnowledge,
    deleteKnowledge
  }
}) 