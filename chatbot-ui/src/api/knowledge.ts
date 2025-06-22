import request from '@/utils/request'
import type { KnowledgeBase, PageResponse } from './types'

export const knowledgeApi = {
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
    },

    /**
     * 下载Excel文件（BIO方式）
     */
    downloadExcelBio: () => {
        return request.get('/ai/knowledge/export/bio', {
            responseType: 'blob'
        })
    },

    /**
     * 下载Excel文件（NIO方式）
     */
    downloadExcelNio: () => {
        return request.get('/ai/knowledge/export/nio', {
            responseType: 'blob'
        })
    },

    /**
     * 下载Excel文件（流式NIO方式）
     */
    downloadExcelStreamingNio: () => {
        return request.get('/ai/knowledge/export/streaming-nio', {
            responseType: 'blob'
        })
    },

    /**
     * 下载CSV文件（流式导出）
     */
    downloadCsv: () => {
        return request.get('/ai/knowledge/export/csv', {
            responseType: 'blob'
        })
    }
}; 