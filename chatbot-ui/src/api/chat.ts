import request from '@/utils/request'

export interface ChatRequest {
  message: string
  sessionId: string
  modelId: string
}

export interface ChatResponse {
  message: string
  modelId: string
  sessionId: string
}

export interface ChatHistoryItem {
  role: string
  message: string
  modelId?: string
}

export const chatApi = {
  // 获取所有会话
  getAllSessions: () => {
    return request.get<string[]>('/ai/chat/sessions')
  },

  // 获取会话历史
  getHistory: (sessionId: string) => {
    return request.get<ChatHistoryItem[]>(`/ai/chat/history/${sessionId}`)
  },

  // 发送消息
  sendMessage: (data: ChatRequest) => {
    return request.post<ChatResponse>('/ai/chat/send', data)
  },

  // 删除会话
  deleteSession: (sessionId: string) => {
    return request.delete(`/ai/chat/sessions/${sessionId}`)
  },

  // 获取可用模型
  getAvailableModels: () => {
    return request.get<string[]>('/ai/chat/models')
  }
} 