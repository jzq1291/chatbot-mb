<template>
  <div class="chat-container">
    <!-- 左侧边栏：包含新建聊天按钮和聊天历史列表 -->
    <div class="sidebar">
      <div class="logo">
        <img src="@/assets/ai-logo.svg" alt="AI Assistant" class="logo-img" />
        <span class="logo-text">AI 助手</span>
      </div>
      <el-button type="primary" @click="createNewChat" class="new-chat-btn">
        新建聊天
      </el-button>
      <div class="chat-history">
        <div
          v-for="sessionId in store.sessions"
          :key="sessionId"
          class="chat-session"
          :class="{ active: sessionId === store.currentSessionId }"
          @click="switchSession(sessionId)"
        >
          <span class="session-title">会话 {{ sessionId.slice(0, 8) }}</span>
          <el-icon class="delete-icon" @click.stop="deleteSession(sessionId)">
            <Close />
          </el-icon>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main">
      <div class="model-selector" v-if="store.currentSessionId">
        <el-select
          v-model="store.selectedModel"
          placeholder="选择模型"
          @change="handleModelChange"
        >
          <el-option
            v-for="model in store.availableModels"
            :key="model"
            :label="model"
            :value="model"
          />
        </el-select>
      </div>
      <div class="messages" ref="messagesContainer">
        <div
          v-for="(message, index) in store.messages"
          :key="index"
          :class="['message', message.role]"
        >
          <div class="message-content" v-html="formatMessage(message.content)"></div>
        </div>
        <!-- 思考中的动画 -->
        <div v-if="store.showThinking" class="message assistant">
          <div class="message-content thinking">
            <div class="thinking-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入消息..."
          @keyup.enter="handleEnterKey"
        />
        <div class="button-group">
          <div class="left-controls">
            <el-checkbox v-model="useStreaming" label="Stream" />
          </div>
          <el-button type="primary" @click="sendMessage" :loading="loading">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/store/chat'
import { Close } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'

// 初始化 store
const store = useChatStore()
const authStore = useAuthStore()
// 创建响应式变量：输入消息
const inputMessage = ref('')
// 创建响应式变量：消息容器引用
const messagesContainer = ref<HTMLElement | null>(null)
// 创建响应式变量：加载状态
const loading = ref(false)
// 创建响应式变量：流式输出开关
const useStreaming = ref(false)

// 处理模型变更
const handleModelChange = (model: string) => {
  store.selectedModel = model
}

// 处理回车键
const handleEnterKey = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 格式化消息内容，将换行符转换为 <br>
const formatMessage = (content: string) => {
  return content.replace(/\n/g, '<br>')
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  const message = inputMessage.value
  inputMessage.value = ''
  loading.value = true

  try {
    if (useStreaming.value) {
      // 使用流式响应
      await store.sendMessageStreaming(message, store.selectedModel, (chunk) => {
        // 每次收到新的数据块时，更新UI并滚动到底部
        nextTick(() => {
          scrollToBottom()
        })
      })
    } else {
      // 使用普通响应
      await store.sendMessage(message, store.selectedModel)
      nextTick(() => {
        scrollToBottom()
      })
    }
  } catch (error) {
    console.error('Failed to send message:', error)
    ElMessage.error('发送消息失败，请重试')
  } finally {
    loading.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 切换会话
const switchSession = async (sessionId: string) => {
  await store.switchSession(sessionId)
  await nextTick()
  scrollToBottom()
}

// 创建新会话
const createNewChat = async () => {
  try {
    await store.createNewChat()
  } catch (error) {
    ElMessage.error('创建新会话失败：' + (error as Error).message)
  }
}

// 删除会话
const deleteSession = async (sessionId: string) => {
  try {
    await store.deleteSession(sessionId)
    ElMessage.success('会话已删除')
  } catch (error) {
    ElMessage.error('删除会话失败：' + (error as Error).message)
  }
}

// 组件挂载时的初始化
onMounted(async () => {
  if (authStore.token) {
    try {
      await Promise.all([
        store.loadSessions(),
        store.loadAvailableModels()
      ])
      // 从本地存储加载流式输出设置
      const savedStreamingSetting = localStorage.getItem('useStreaming')
      if (savedStreamingSetting !== null) {
        useStreaming.value = JSON.parse(savedStreamingSetting)
      }
    } catch (error: any) {
      if (error.response?.status !== 403) {
        console.error('Failed to load initial data:', error)
      }
    }
  }
})

// 监听流式输出设置变化
watch(useStreaming, (newValue) => {
  // 保存设置到本地存储
  localStorage.setItem('useStreaming', JSON.stringify(newValue))
})
</script>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  background-color: #f5f7fa;
}

.sidebar {
  width: 200px;
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.logo {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.logo-img {
  width: 32px;
  height: 32px;
  margin-right: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.new-chat-btn {
  margin-bottom: 20px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
}

.chat-session {
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f5f7fa;
}

.chat-session:hover {
  background-color: #ecf5ff;
}

.chat-session.active {
  background-color: #ecf5ff;
  border: 1px solid #409EFF;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-icon {
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
  color: #909399;
  font-size: 16px;
}

.delete-icon:hover {
  color: #F56C6C;
}

.chat-session:hover .delete-icon {
  opacity: 1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.model-selector {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.message {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  align-items: flex-end;
}

.message.assistant {
  align-items: flex-start;
}

.message-content {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 8px;
  word-wrap: break-word;
  line-height: 1.6;
  font-size: 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.user .message-content {
  background-color: #409EFF;
  color: #fff;
  border-top-right-radius: 2px;
}

.assistant .message-content {
  background-color: #f5f7fa;
  color: #303133;
  border-top-left-radius: 2px;
}

.input-area {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.button-group {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  transition: all 0.3s;
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

:deep(.el-checkbox__label) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-button--primary) {
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.message-content.thinking {
  min-width: 60px;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thinking-dots {
  display: flex;
  align-items: center;
  gap: 4px;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  background-color: #909399;
  border-radius: 50%;
  animation: thinking 1.4s infinite ease-in-out;
}

.thinking-dots span:nth-child(1) {
  animation-delay: 0s;
}

.thinking-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes thinking {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.6;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style> 