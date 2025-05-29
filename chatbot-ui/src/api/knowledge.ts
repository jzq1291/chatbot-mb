import request from '@/utils/request'
import type { KnowledgeBase, PageResponse } from './types'

export const knowledgeApi = {
    // getAll: (page: number, size: number) => {
    //     return request.get<PageResponse<KnowledgeBase>>('/ai/knowledge/search', {
    //         params: { page, size }
    //     })
    // },

    getKnowledgeList: (page: number, size: number) => {
        return request.get<PageResponse<KnowledgeBase>>('/ai/knowledge', {
            params: { page, size }
        })
    },

    searchKnowledge: (keyword: string, page: number, size: number) => {
        return request.get<PageResponse<KnowledgeBase>>('/ai/knowledge/search', {
            params: { keyword, page, size }
        })
    },

    findByCategory: (category: string, page: number, size: number) => {
        return request.get<PageResponse<KnowledgeBase>>(`/ai/knowledge/category/${category}`, {
            params: { page, size }
        })
    },

    addKnowledge: (knowledge: KnowledgeBase) => {
        return request.post<KnowledgeBase>('/ai/knowledge', knowledge)
    },

    updateKnowledge: (knowledge: KnowledgeBase) => {
        return request.put<KnowledgeBase>(`/ai/knowledge/${knowledge.id}`, knowledge)
    },

    deleteKnowledge: (id: number) => {
        return request.delete(`/ai/knowledge/${id}`)
    },

    batchImportKnowledge: (knowledgeList: KnowledgeBase[]) => {
        return request.post('/ai/knowledge/batch-import', knowledgeList)
    }
}; 