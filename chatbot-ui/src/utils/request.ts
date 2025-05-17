import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8082',
  timeout: 300000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 不需要认证的接口
const publicApis = ['/ai/auth/login', '/ai/auth/register']

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    console.log('Request config:', {
      url: config.url,
      method: config.method,
      headers: config.headers,
      data: config.data
    })
    
    // 如果是公开接口，不添加 token
    if (config.url && publicApis.includes(config.url)) {
      console.log('Public API request:', config.url)
      return config
    }

    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    console.error('Response error:', error)
    const responseData = error.response?.data
    const errorCode = responseData?.errorCode
    const message = responseData?.message || '请求失败'
    
    // 统一显示后端返回的错误信息
    ElMessage.error(message)
    
    // 如果是认证相关错误，清除token并跳转到登录页
    if (errorCode?.startsWith('AUTH_')) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('roles')
      // 使用 router 进行导航，而不是直接修改 location
      router.push('/login')
    }
    
    return Promise.reject(error)
  }
)

// 封装 GET 请求
const get = <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return service.get(url, config)
}

// 封装 POST 请求
const post = <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return service.post(url, data, config)
}

// 封装 PUT 请求
const put = <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return service.put(url, data, config)
}

// 封装 DELETE 请求
const del = <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return service.delete(url, config)
}

export default {
  get,
  post,
  put,
  delete: del
} 