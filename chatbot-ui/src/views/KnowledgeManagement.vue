<template>
  <div class="knowledge-management">
    <div class="header">
      <h2>知识库管理</h2>
      <div class="header-buttons">
        <el-upload
          class="upload-demo"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          accept=".xlsx,.xls"
        >
          <el-button type="primary">批量导入</el-button>
        </el-upload>
        
        <!-- 下载下拉菜单 -->
        <el-dropdown @command="handleDownload" :loading="downloading">
          <el-button type="success" :loading="downloading">
            下载数据
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="bio">下载Excel (BIO)</el-dropdown-item>
              <el-dropdown-item command="nio">下载Excel (NIO)</el-dropdown-item>
              <el-dropdown-item command="streaming-nio">下载Excel (流式NIO)</el-dropdown-item>
              <el-dropdown-item command="csv">导出CSV</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <el-button type="primary" @click="showAddDialog">添加知识</el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="store.searchKeyword"
        placeholder="搜索知识..."
        @input="handleSearch"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="knowledge-list">
      <el-table :data="store.knowledgeList" style="width: 100%" v-loading="store.loading">
        <el-table-column prop="title" label="标题">
          <template #default="{ row }">
            <el-link type="primary" @click="showEditDialog(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10]"
          :total="totalElements"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 添加知识对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑知识' : '添加知识'"
      width="50%"
    >
      <el-form :model="newKnowledge" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="newKnowledge.title" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="newKnowledge.category" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="newKnowledge.content"
            type="textarea"
            :rows="10"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="store.loading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量导入预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="批量导入预览"
      width="70%"
    >
      <div class="preview-header">
        <span>共 {{ previewData.length }} 条数据</span>
        <el-button type="primary" @click="handleBatchImport" :loading="importing">
          确认导入
        </el-button>
      </div>
      <el-table :data="previewData" style="width: 100%" height="400">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="内容" width="300">
          <template #default="{ row }">
            <div class="content-preview">
              {{ row.content.substring(0, 100) }}{{ row.content.length > 100 ? '...' : '' }}
              <el-button type="primary" link @click="showContentPreview(row)">
                查看完整内容
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 内容预览对话框 -->
    <el-dialog
      v-model="contentPreviewVisible"
      title="内容预览"
      width="50%"
    >
      <div class="content-preview-text">
        {{ currentPreviewContent }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, ArrowDown } from '@element-plus/icons-vue';
import { useKnowledgeStore } from '@/store/knowledge';
import { knowledgeApi } from '@/api/knowledge';
import type { KnowledgeBase } from '@/api/types';
import { useAuthStore } from '@/store/auth'
import * as XLSX from 'xlsx';

const store = useKnowledgeStore();
const authStore = useAuthStore();
const dialogVisible = ref(false);
const isEditMode = ref(false);
const currentPage = ref(1);
const pageSize = ref(12);
const totalElements = ref(0);

const newKnowledge = ref<KnowledgeBase>({
  title: '',
  content: '',
  category: ''
});

const previewDialogVisible = ref(false);
const contentPreviewVisible = ref(false);
const previewData = ref<KnowledgeBase[]>([]);
const currentPreviewContent = ref('');
const importing = ref(false);
const downloading = ref(false);

// 更新分页信息
const updatePagination = (response: any) => {
  totalElements.value = response.totalElements;
  currentPage.value = response.currentPage;
};

// 搜索知识
const handleSearch = async () => {
  try {
    currentPage.value = 1;
    const response = await store.searchKnowledge(store.searchKeyword, currentPage.value, pageSize.value);
    updatePagination(response);
  } catch (error) {
    ElMessage.error('搜索失败');
  }
};

// 处理页码变化
const handlePageChange = async (page: number) => {
  try {
    let response;
    if (store.searchKeyword) {
      response = await store.searchKnowledge(store.searchKeyword, page, pageSize.value);
    } else {
      response = await store.loadKnowledge(page, pageSize.value);
    }
    updatePagination(response);
  } catch (error) {
    ElMessage.error('加载失败');
  }
};

// 显示添加对话框
const showAddDialog = () => {
  newKnowledge.value = {
    title: '',
    content: '',
    category: ''
  };
  isEditMode.value = false;
  dialogVisible.value = true;
};

// 显示编辑对话框
const showEditDialog = (row: KnowledgeBase) => {
  newKnowledge.value = { ...row };
  isEditMode.value = true;
  dialogVisible.value = true;
};

// 添加或编辑知识
const handleSave = async () => {
  try {
    let response;
    if (isEditMode.value) {
      response = await store.updateKnowledge(newKnowledge.value);
      ElMessage.success('保存成功');
    } else {
      response = await store.addKnowledge(newKnowledge.value);
      ElMessage.success('添加成功');
    }
    updatePagination(response);
    dialogVisible.value = false;
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(isEditMode.value ? '保存失败' : '添加失败');
    }
  }
};

// 删除知识
const handleDelete = async (row: KnowledgeBase) => {
  if (!row.id) return;
  
  try {
    await ElMessageBox.confirm('确定要删除这条知识吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const response = await store.deleteKnowledge(row.id);
    ElMessage.success('删除成功');
    updatePagination(response);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && error instanceof Error) {
      ElMessage.error('删除失败');
    }
  }
};

// 处理文件选择
const handleFileChange = async (file: any) => {
  if (!file) return;
  
  try {
    const data = await readExcelFile(file.raw);
    if (data.length > 200) {
      ElMessage.error('导入数据不能超过200条');
      return;
    }
    
    previewData.value = data;
    previewDialogVisible.value = true;
  } catch (error) {
    ElMessage.error('文件读取失败');
    console.error('File reading error:', error);
  }
};

// 读取Excel文件
const readExcelFile = (file: File): Promise<KnowledgeBase[]> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = e.target?.result;
        const workbook = XLSX.read(data, { type: 'binary' });
        const sheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[sheetName];
        const jsonData = XLSX.utils.sheet_to_json(worksheet);
        
        const knowledgeData = jsonData.map((row: any) => ({
          title: row.title || '',
          content: row.content || '',
          category: row.category || ''
        }));
        
        resolve(knowledgeData);
      } catch (error) {
        reject(error);
      }
    };
    reader.onerror = reject;
    reader.readAsBinaryString(file);
  });
};

// 显示内容预览
const showContentPreview = (row: KnowledgeBase) => {
  currentPreviewContent.value = row.content;
  contentPreviewVisible.value = true;
};

// 处理批量导入
const handleBatchImport = async () => {
  if (previewData.value.length === 0) {
    ElMessage.warning('没有数据可导入');
    return;
  }

  importing.value = true;
  try {
    const response = await store.batchImportKnowledge(previewData.value);
    ElMessage.success('导入成功');
    previewDialogVisible.value = false;
    updatePagination(response);
  } catch (error) {
    ElMessage.error('导入失败');
    console.error('Import error:', error);
  } finally {
    importing.value = false;
  }
};

// 处理下载
const handleDownload = async (command: string) => {
  downloading.value = true;
  try {
    let response: any;
    
    switch (command) {
      case 'bio':
        response = await knowledgeApi.downloadExcelBio();
        break;
      case 'nio':
        response = await knowledgeApi.downloadExcelNio();
        break;
      case 'streaming-nio':
        response = await knowledgeApi.downloadExcelStreamingNio();
        break;
      case 'csv':
        response = await knowledgeApi.downloadCsv();
        break;
      default:
        throw new Error('无效的下载方式');
    }
    
    // 根据下载类型设置不同的MIME类型
    let mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    let fileExtension = 'xlsx';
    let successMessage = `${command.toUpperCase()}方式下载成功`;
    
    if (command === 'csv') {
      mimeType = 'text/csv;charset=utf-8;';
      fileExtension = 'csv';
      successMessage = 'CSV流式导出成功';
    }
    
    // 创建下载链接
    const blob = new Blob([response.data], { type: mimeType });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    
    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let filename = `知识库数据_${command.toUpperCase()}_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.${fileExtension}`;
    
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1].replace(/['"]/g, '');
      }
    }
    
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    ElMessage.success(successMessage);
  } catch (error) {
    ElMessage.error('下载失败');
    console.error('Download error:', error);
  } finally {
    downloading.value = false;
  }
};

// 组件挂载时的初始化
onMounted(async () => {
  if (authStore.token) {
    try {
      const response = await store.loadKnowledge(1, pageSize.value);
      updatePagination(response);
    } catch (error: any) {
      if (error.response?.status !== 403) {
        ElMessage.error('加载知识库列表失败：' + (error as Error).message)
      }
    }
  }
})
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
}

.knowledge-list {
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.content-preview {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.content-preview-text {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}
</style> 