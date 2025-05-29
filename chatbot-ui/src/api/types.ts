export interface KnowledgeBase {
  id?: number;
  title: string;
  content: string;
  category: string;
}

export interface PageResponse<T> {
  content: T[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  email: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  roles: string[];
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_KNOWLEDGEMANAGER';

export const hasRole = (roles: UserRole[], role: UserRole): boolean => {
  return roles.includes(role);
};

export const hasAnyRole = (roles: UserRole[], requiredRoles: UserRole[]): boolean => {
  return requiredRoles.some(role => roles.includes(role));
}; 