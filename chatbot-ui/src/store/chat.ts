import { defineStore } from 'pinia'
import { ref, nextTick } from 'vue'
import { chatApi, type ChatResponse } from '@/api/chat'

// 定义消息接口
export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  modelId?: string
}

// 定义聊天会话接口
export interface ChatSession {
  id: string
  title: string
  messages: Message[]
  lastUpdated: number
  selectedModel: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

// 创建聊天 store
export const useChatStore = defineStore('chat', () => {
  // 状态
  const sessions = ref<string[]>([])
  const currentSessionId = ref<string>('')
  const messages = ref<ChatMessage[]>([])
  const availableModels = ref<string[]>([])
  const selectedModel = ref<string>('')
  const isStreaming = ref<boolean>(false)
  const streamingContent = ref<string>('')
  const showThinking = ref<boolean>(false)

  // 加载可用模型列表
  const loadAvailableModels = async () => {
    try {
      const models = await chatApi.getAvailableModels()
      availableModels.value = models
      if (models.length > 0) {
        selectedModel.value = models[0]
      }
    } catch (error) {
      console.error('Failed to load available models:', error)
      throw error
    }
  }

  // 加载会话列表
  const loadSessions = async () => {
    try {
      const response = await chatApi.getAllSessions()
      // 合并后端返回的会话和前端未发送消息的会话
      const backendSessions = new Set(response)
      const frontendSessions = sessions.value.filter(id => !backendSessions.has(id))
      sessions.value = [...frontendSessions, ...response]
      
      if (sessions.value.length > 0) {
        currentSessionId.value = sessions.value[0]
        // 只加载后端存在的会话的历史记录
        if (backendSessions.has(currentSessionId.value)) {
          const history = await chatApi.getHistory(currentSessionId.value)
          messages.value = history.map((item: any) => ({
            role: item.role || 'assistant',
            content: item.message
          }))
        } else {
          messages.value = []
        }
      }
    } catch (error) {
      console.error('Failed to load sessions:', error)
      throw error
    }
  }

  // 切换会话
  const switchSession = async (sessionId: string) => {
    try {
      // 重置消息状态
      messages.value = []
      const history = await chatApi.getHistory(sessionId)
      messages.value = history.map((item: any) => ({
        role: item.role || 'assistant',
        content: item.message
      }))
      currentSessionId.value = sessionId
      // 确保流式响应状态被重置
      isStreaming.value = false
      streamingContent.value = ''
      showThinking.value = false
    } catch (error) {
      console.error('Failed to switch session:', error)
      throw error
    }
  }

  // 创建新会话
  const createNewChat = async () => {
    try {
      // 在前端生成新的会话ID
      const newSessionId = crypto.randomUUID()
      sessions.value.unshift(newSessionId)
      currentSessionId.value = newSessionId
      messages.value = []
      // 确保流式响应状态被重置
      isStreaming.value = false
      streamingContent.value = ''
      showThinking.value = false
    } catch (error) {
      console.error('Failed to create new session:', error)
      throw error
    }
  }

  // 删除会话
  const deleteSession = async (sessionId: string) => {
    try {
      await chatApi.deleteSession(sessionId)
      sessions.value = sessions.value.filter(id => id !== sessionId)
      if (currentSessionId.value === sessionId) {
        if (sessions.value.length > 0) {
          await switchSession(sessions.value[0])
        } else {
          await createNewChat()
        }
      }
    } catch (error) {
      console.error('Failed to delete session:', error)
      throw error
    }
  }

  // 发送消息
  const sendMessage = async (content: string, model: string) => {
    if (!currentSessionId.value) {
      await createNewChat()
    }

    const userMessage: ChatMessage = {
      role: 'user',
      content
    }

    messages.value.push(userMessage)
    showThinking.value = true

    try {
      const response = await chatApi.sendMessage({
        message: content,
        sessionId: currentSessionId.value,
        modelId: model
      })
      const assistantMessage: ChatMessage = {
        role: 'assistant',
        content: response.message
      }
      messages.value.push(assistantMessage)
    } catch (error) {
      console.error('Failed to send message:', error)
      throw error
    } finally {
      showThinking.value = false
    }
  }

  // 发送流式消息
  const sendMessageStreaming = async (content: string, model: string, onChunk: (chunk: string) => void) => {
    if (!currentSessionId.value) {
      await createNewChat()
    }

    // 添加用户消息
    const userMessage: ChatMessage = {
      role: 'user',
      content
    }
    messages.value.push(userMessage)
    showThinking.value = true

    try {
      await chatApi.sendMessageStreaming({
        message: content,
        sessionId: currentSessionId.value,
        modelId: model
      }, async (response) => {
        // 获取最后一条消息
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage && lastMessage.role === 'assistant') {
          // 将新消息追加到现有内容后面
          lastMessage.content += response.message
        } else {
          // 如果是第一条响应，创建新的助手消息
          const assistantMessage: ChatMessage = {
            role: 'assistant',
            content: response.message
          }
          messages.value.push(assistantMessage)
          // 收到第一条响应时，隐藏思考动画
          showThinking.value = false
        }
        // 等待 DOM 更新完成
        await nextTick()
        if (onChunk) {
          onChunk(response.message)
        }
      })
    } catch (error) {
      showThinking.value = false
      throw error
    }
  }

  // 重置状态
  const resetState = () => {
    sessions.value = []
    currentSessionId.value = ''
    messages.value = []
    availableModels.value = []
    selectedModel.value = ''
    isStreaming.value = false
    streamingContent.value = ''
    showThinking.value = false
  }

  return {
    sessions,
    currentSessionId,
    messages,
    availableModels,
    selectedModel,
    showThinking,
    loadAvailableModels,
    loadSessions,
    switchSession,
    createNewChat,
    deleteSession,
    sendMessage,
    sendMessageStreaming,
    resetState
  }
}) 