import request from '@/utils/request'
import type { AxiosProgressEvent } from 'axios'

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

  // 发送流式消息
  sendMessageStreaming: (data: ChatRequest, onChunk: (chunk: ChatResponse) => void) => {
    let buffer = '' // 用于存储未完成的数据
    let lastMessage = '' // 记录上一次的完整消息
    return request.post<ChatResponse>('/ai/chat/send/reactive', data, {
      responseType: 'stream',
      onDownloadProgress: (progressEvent: AxiosProgressEvent) => {
        const chunk = progressEvent.event?.target?.response
        if (chunk) {
          try {
            // 将新数据添加到缓冲区
            buffer += chunk
            // 处理完整的消息
            const messages = buffer.split('\n\n')
            // 保留最后一个可能不完整的消息
            buffer = messages.pop() || ''
            
            // 处理完整的消息
            for (const message of messages) {
              if (message.startsWith('data:')) {
                try {
                  const jsonStr = message.slice(5).trim() // 跳过 'data:' 并去除空白字符
                  if (jsonStr && jsonStr !== 'data:') { // 确保不是空字符串或单独的 data:
                    const response = JSON.parse(jsonStr)
                    // 只处理新的内容
                    if (response.message && response.message !== lastMessage) {
                      const newContent = response.message.slice(lastMessage.length)
                      onChunk({
                        ...response,
                        message: newContent
                      })
                      lastMessage = response.message
                    }
                  }
                } catch (e) {
                  console.error('Failed to parse streaming chunk:', e, 'chunk:', message)
                }
              }
            }
          } catch (e) {
            console.error('Failed to process streaming chunks:', e)
          }
        }
      }
    })
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