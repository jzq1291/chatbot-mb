import request from '@/utils/request'

export interface KnowledgeBase {
    id?: number;
    title: string;
    content: string;
    category: string;
}

export const knowledgeApi = {
    // 添加知识
    addKnowledge: (knowledge: KnowledgeBase) => {
        return request.post<KnowledgeBase>('/ai/knowledge', knowledge)
    },

    // 搜索知识
    searchKnowledge: (keyword: string) => {
        return request.get<KnowledgeBase[]>('/ai/knowledge/search', {
            params: { keyword }
        })
    },

    // 按分类获取知识
    getByCategory: (category: string) => {
        return request.get<KnowledgeBase[]>(`/ai/knowledge/category/${category}`)
    },

    // 删除知识
    deleteKnowledge: (id: number) => {
        return request.delete(`/ai/knowledge/${id}`)
    },

    // 更新知识
    updateKnowledge: (knowledge: KnowledgeBase) => {
        if (!knowledge.id) throw new Error('知识ID不能为空')
        return request.put<KnowledgeBase>(`/ai/knowledge/${knowledge.id}`, knowledge)
    }
}; 