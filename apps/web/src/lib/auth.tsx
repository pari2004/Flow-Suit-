import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { api } from './api';
import { tokenStore } from './tokenStore';
import type { AuthResponse, Role, UserSummary } from '../types';

interface AuthContextValue {
  user: UserSummary | null;
  isAuthenticated: boolean;
  hasRole: (...roles: Role[]) => boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  acceptInvitation: (input: AcceptInput) => Promise<void>;
  logout: () => Promise<void>;
}

interface RegisterInput {
  name: string;
  email: string;
  password: string;
  organizationName?: string;
}

interface AcceptInput {
  token: string;
  name: string;
  password: string;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(() => tokenStore.getUser());

  const value = useMemo<AuthContextValue>(() => {
    const persist = (auth: AuthResponse) => {
      tokenStore.set(auth.accessToken, auth.refreshToken, auth.user);
      setUser(auth.user);
    };

    return {
      user,
      isAuthenticated: !!user,
      hasRole: (...roles: Role[]) => !!user && roles.includes(user.role),
      login: async (email, password) => {
        const { data } = await api.post<AuthResponse>('/api/v1/auth/login', { email, password });
        persist(data);
      },
      register: async (input) => {
        const { data } = await api.post<AuthResponse>('/api/v1/auth/register', input);
        persist(data);
      },
      acceptInvitation: async (input) => {
        const { data } = await api.post<AuthResponse>('/api/v1/auth/accept-invitation', input);
        persist(data);
      },
      logout: async () => {
        const refreshToken = tokenStore.getRefresh();
        try {
          if (refreshToken) await api.post('/api/v1/auth/logout', { refreshToken });
        } finally {
          tokenStore.clear();
          setUser(null);
        }
      },
    };
  }, [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
