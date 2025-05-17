<template>
  <div v-if="authStore.token">
    <el-container class="layout-container">
      <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
        <div class="sidebar-header">
          <el-icon class="toggle-icon" @click="toggleSidebar">
            <component :is="isCollapse ? Expand : Fold" />
          </el-icon>
        </div>
        <el-menu
          :router="true"
          :default-active="$route.path"
          class="el-menu-vertical"
          :collapse="isCollapse"
        >
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>聊天</template>
          </el-menu-item>
          <el-menu-item v-if="authStore.checkAnyRole(['ROLE_ADMIN', 'ROLE_KNOWLEDGEMANAGER'])" index="/knowledge">
            <el-icon><Collection /></el-icon>
            <template #title>知识库管理</template>
          </el-menu-item>
          <el-menu-item v-if="authStore.checkRole('ROLE_ADMIN')" index="/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
        </el-menu>
        <div class="user-info">
          <span v-if="!isCollapse">{{ authStore.username }}</span>
          <el-button link @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span v-if="!isCollapse">退出</span>
          </el-button>
        </div>
      </el-aside>
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </div>
  <router-view v-else></router-view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/store/auth'
import { useRouter } from 'vue-router'
import { ChatDotRound, Collection, User, Fold, Expand, SwitchButton } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const isCollapse = ref(true)

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleLogout = async () => {
  try {
    await authStore.logout()
  } catch (error) {
    console.error('Logout error:', error)
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  transition: width 0.3s;
  background-color: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e6e6e6;
}

.toggle-icon {
  font-size: 20px;
  cursor: pointer;
  color: #909399;
  transition: color 0.3s;
}

.toggle-icon:hover {
  color: #409EFF;
}

.el-menu-vertical {
  flex: 1;
  border-right: none;
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: 200px;
}

.user-info {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #e6e6e6;
}

.user-info span {
  color: #606266;
}

.el-main {
  padding: 0;
  background-color: #f5f7fa;
}
</style> 