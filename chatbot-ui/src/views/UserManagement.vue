<template>
  <div class="user-management">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="showAddDialog">添加用户</el-button>
    </div>

    <div class="user-list">
      <el-table :data="userStore.userList" v-loading="userStore.loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="roles" label="角色">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" :type="role === 'ROLE_ADMIN' ? 'danger' : role === 'ROLE_KNOWLEDGEMANAGER' ? 'warning' : 'success'" class="role-tag">
              {{ role === 'ROLE_ADMIN' ? '管理员' : role === 'ROLE_KNOWLEDGEMANAGER' ? '知识库管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑用户' : '添加用户'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="newUser"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="newUser.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="newUser.email" />
        </el-form-item>
        <el-form-item label="密码" prop="password" :rules="isEditMode ? editPasswordRules : rules.password" :required="!isEditMode">
          <el-input v-model="newUser.password" type="password" :placeholder="isEditMode ? '不修改请留空' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-select v-model="newUser.roles" multiple style="width: 100%">
            <el-option label="管理员" value="ROLE_ADMIN" />
            <el-option label="普通用户" value="ROLE_USER" />
            <el-option label="知识库管理员" value="ROLE_KNOWLEDGEMANAGER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="userStore.loading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useAuthStore } from '@/store/auth'
import type { User } from '@/api/user'

const userStore = useUserStore()
const authStore = useAuthStore()
const dialogVisible = ref(false)
const isEditMode = ref(false)

const formRef = ref<FormInstance>()
const newUser = ref<User>({
  username: '',
  email: '',
  password: '',
  roles: ['ROLE_USER']
})

const rules = ref<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  roles: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
})

const editPasswordRules = [
  { 
    validator: (rule: any, value: string, callback: any) => {
      if (value && (value.length < 6 || value.length > 20)) {
        callback(new Error('长度在 6 到 20 个字符'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }
]

const showAddDialog = () => {
  isEditMode.value = false
  newUser.value = {
    username: '',
    email: '',
    password: '',
    roles: ['ROLE_USER']
  }
  dialogVisible.value = true
}

const showEditDialog = (row: User) => {
  isEditMode.value = true
  newUser.value = { 
    ...row,
    password: '' // 清空密码字段
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const userToSave = { ...newUser.value }
        // 如果是编辑模式且密码为空，则删除密码字段
        if (isEditMode.value && !userToSave.password) {
          delete userToSave.password
        }
        
        if (isEditMode.value) {
          await userStore.updateUser(userToSave)
          ElMessage.success('更新成功')
        } else {
          await userStore.createUser(userToSave)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
      } catch (error) {
        console.error('保存失败:', error)
      }
    }
  })
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个用户吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await userStore.deleteUser(id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(async () => {
  if (authStore.token) {
    try {
      await userStore.loadUsers()
    } catch (error: any) {
      if (error.response?.status !== 403) {
        ElMessage.error('加载用户列表失败：' + (error as Error).message)
      }
    }
  }
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.user-list {
  margin-top: 20px;
}

.role-tag {
  margin-right: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 