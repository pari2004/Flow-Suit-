import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { tokenStore } from './tokenStore';
import type { ApiError } from '../types';

const baseURL = import.meta.env.VITE_API_URL ?? '';

export const api = axios.create({ baseURL });

api.interceptors.request.use((config) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) throw new Error('No refresh token');
  const { data } = await axios.post(`${baseURL}/api/v1/auth/refresh`, { refreshToken });
  tokenStore.setTokens(data.accessToken, data.refreshToken);
  return data.accessToken as string;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;
    const url = original?.url ?? '';

    const isAuthCall = url.includes('/api/v1/auth/login') ||
      url.includes('/api/v1/auth/refresh') ||
      url.includes('/api/v1/auth/register');

    if (status === 401 && !original._retry && !isAuthCall && tokenStore.getRefresh()) {
      original._retry = true;
      try {
        refreshPromise = refreshPromise ?? refreshAccessToken();
        const newToken = await refreshPromise;
        refreshPromise = null;
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      } catch (refreshError) {
        refreshPromise = null;
        tokenStore.clear();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);

/** Extracts the backend's { code, message, status } message, with a sensible fallback. */
export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  const axiosError = error as AxiosError<ApiError>;
  return axiosError.response?.data?.message ?? fallback;
}
