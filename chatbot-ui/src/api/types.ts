export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  email?: string
}

export interface AuthResponse {
  token: string
  username: string
  roles: string[]
}

export type UserRole = 'ROLE_ADMIN' | 'ROLE_USER' | 'ROLE_KNOWLEDGEMANAGER'

export const hasRole = (roles: string[], role: UserRole): boolean => {
  return roles.includes(role)
}

export const hasAnyRole = (roles: string[], requiredRoles: UserRole[]): boolean => {
  return requiredRoles.some(role => roles.includes(role))
} 