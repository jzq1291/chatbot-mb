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
    let processedMessages = new Set() // 用于跟踪已处理的消息
    return request.post<ChatResponse>('/ai/chat/send/reactive', data, {
      responseType: 'stream',
      onDownloadProgress: (progressEvent: AxiosProgressEvent) => {
        const chunk = progressEvent.event?.target?.response
        if (chunk) {
          try {
            // 将新数据添加到缓冲区
            buffer += chunk
            
            // 处理所有可能的完整消息
            while (buffer.includes('data:')) {
              // 找到第一个 data: 的位置
              const dataStart = buffer.indexOf('data:')
              if (dataStart === -1) break
              
              // 找到下一个 data: 的位置，如果没有则使用缓冲区末尾
              const nextDataStart = buffer.indexOf('data:', dataStart + 5)
              const messageEnd = nextDataStart === -1 ? buffer.length : nextDataStart
              
              // 提取当前消息
              const message = buffer.slice(dataStart, messageEnd).trim()
              buffer = buffer.slice(messageEnd)
              
              if (message.startsWith('data:')) {
                try {
                  const jsonStr = message.slice(5).trim() // 跳过 'data:' 并去除空白字符
                  if (jsonStr && jsonStr !== 'data:') { // 确保不是空字符串或单独的 data:
                    const response = JSON.parse(jsonStr)
                    // 使用消息内容、会话ID和序列号组合作为唯一标识
                    const messageKey = `${response.message}-${response.sessionId}-${response.sequence}`
                    if (!processedMessages.has(messageKey)) {
                      processedMessages.add(messageKey)
                      onChunk(response)
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