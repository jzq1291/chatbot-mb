import request from '@/utils/request'

export interface KnowledgeBase {
    id?: number;
    title: string;
    content: string;
    category: string;
}

export interface PageResponse<T> {
    content: T[];
    currentPage: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
}

export const knowledgeApi = {
    getAll: (page: number, size: number) => {
        return request.get<PageResponse<KnowledgeBase>>('/ai/knowledge', {
            params: { page, size }
        })
    },

    search: (keyword: string, page: number, size: number) => {
        return request.get<PageResponse<KnowledgeBase>>('/ai/knowledge/search', {
            params: { keyword, page, size }
        })
    },

    getByCategory: (category: string, page: number, size: number) => {
        return request.get<PageResponse<KnowledgeBase>>(`/ai/knowledge/category/${category}`, {
            params: { page, size }
        })
    },

    add: (knowledge: KnowledgeBase) => {
        return request.post<KnowledgeBase>('/ai/knowledge', knowledge)
    },

    update: (id: number, knowledge: KnowledgeBase) => {
        return request.put<KnowledgeBase>(`/ai/knowledge/${id}`, knowledge)
    },

    delete: (id: number) => {
        return request.delete(`/ai/knowledge/${id}`)
    }
}; 