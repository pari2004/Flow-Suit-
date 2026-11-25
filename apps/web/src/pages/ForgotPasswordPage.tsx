import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { api, errorMessage } from '../lib/api';
import { AuthShell } from './LoginPage';
import type { MessageResponse } from '../types';

interface FormValues {
  email: string;
}

export default function ForgotPasswordPage() {
  const { isAuthenticated } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<FormValues>();

  if (isAuthenticated) return <Navigate to="/" replace />;

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      const { data } = await api.post<MessageResponse>('/api/v1/auth/forgot-password', {
        email: values.email,
      });
      setMessage(data.message);
    } catch (e) {
      setError(errorMessage(e, 'Could not send the reset link'));
    }
  };

  return (
    <AuthShell title="Reset your password">
      {message ? (
        <div className="space-y-4">
          <p className="rounded bg-green-50 px-3 py-2 text-sm text-green-700">{message}</p>
          <Link to="/login" className="btn-primary block w-full text-center">
            Back to sign in
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          <p className="text-sm text-gray-600">
            Enter your account email and we'll send you a link to reset your password.
          </p>
          <div>
            <label className="label">Email</label>
            <input className="input" type="email" {...register('email', { required: true })} />
          </div>
          <button className="btn-primary w-full" disabled={formState.isSubmitting}>
            {formState.isSubmitting ? 'Sending…' : 'Send reset link'}
          </button>
        </form>
      )}
      <p className="mt-4 text-center text-sm text-gray-600">
        Remembered it?{' '}
        <Link to="/login" className="font-medium text-brand-600">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
