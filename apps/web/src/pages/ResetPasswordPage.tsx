import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { api, errorMessage } from '../lib/api';
import { AuthShell } from './LoginPage';
import type { MessageResponse } from '../types';

interface FormValues {
  newPassword: string;
  confirmPassword: string;
}

export default function ResetPasswordPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState, watch } = useForm<FormValues>();

  if (isAuthenticated) return <Navigate to="/" replace />;

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      await api.post<MessageResponse>('/api/v1/auth/reset-password', {
        token,
        newPassword: values.newPassword,
      });
      navigate('/login', {
        replace: true,
        state: { notice: 'Your password has been reset. Please sign in.' },
      });
    } catch (e) {
      setError(errorMessage(e, 'Could not reset your password. The link may be expired.'));
    }
  };

  return (
    <AuthShell title="Choose a new password">
      {!token ? (
        <p className="text-sm text-red-700">Missing reset token in the link.</p>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          <div>
            <label className="label">New password</label>
            <input
              className="input"
              type="password"
              {...register('newPassword', { required: true, minLength: 8 })}
            />
          </div>
          <div>
            <label className="label">Confirm password</label>
            <input
              className="input"
              type="password"
              {...register('confirmPassword', {
                required: true,
                validate: (value) => value === watch('newPassword') || 'Passwords do not match',
              })}
            />
            {formState.errors.confirmPassword && (
              <p className="mt-1 text-sm text-red-700">{formState.errors.confirmPassword.message}</p>
            )}
          </div>
          <button className="btn-primary w-full" disabled={formState.isSubmitting}>
            {formState.isSubmitting ? 'Resetting…' : 'Reset password'}
          </button>
        </form>
      )}
      <p className="mt-4 text-center text-sm text-gray-600">
        <Link to="/login" className="font-medium text-brand-600">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
