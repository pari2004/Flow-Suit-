export type Role = 'OWNER' | 'ADMIN' | 'MANAGER' | 'MEMBER';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';
export type PlanCode = 'FREE' | 'STARTER' | 'PROFESSIONAL';
export type SubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELLED' | 'EXPIRED';

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  organizationId: string;
  organizationName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  status: ProjectStatus;
  createdById?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  status: TaskStatus;
  assigneeId?: string;
  createdById?: string;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Customer {
  id: string;
  name: string;
  email?: string;
  phone?: string;
  company?: string;
  projectId?: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Member {
  membershipId: string;
  userId: string;
  name: string;
  email: string;
  role: Role;
  status: string;
  joinedAt: string;
}

export interface Plan {
  code: PlanCode;
  name: string;
  priceInr: number;
  seatLimit: number;
  projectLimit: number | null;
  apiRequestLimit: number;
  entitlements: Record<string, unknown>;
}

export interface Subscription {
  planCode: PlanCode;
  planName: string;
  status: SubscriptionStatus;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd: boolean;
}

export interface Metric {
  used: number;
  limit: number | null;
  unlimited: boolean;
}

export interface UsageDashboard {
  planCode: PlanCode;
  planName: string;
  subscriptionStatus: SubscriptionStatus;
  seats: Metric;
  projects: Metric;
  apiRequests: Metric;
  advancedAnalytics: boolean;
}

export interface AuditLog {
  id: string;
  actorId?: string;
  action: string;
  targetType?: string;
  targetId?: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}

export interface ApiError {
  code: string;
  message: string;
  status: number;
}

export interface MessageResponse {
  message: string;
}
