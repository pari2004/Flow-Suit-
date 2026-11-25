import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { errorMessage } from '../lib/api';

interface FormValues {
  email: string;
  password: string;
}

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const notice = (location.state as { notice?: string } | null)?.notice ?? null;
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<FormValues>();

  if (isAuthenticated) return <Navigate to="/" replace />;

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      await login(values.email, values.password);
      navigate('/');
    } catch (e) {
      setError(errorMessage(e, 'Invalid email or password'));
    }
  };

  return (
    <AuthShell title="Sign in to FlowSuite">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {notice && <p className="rounded bg-green-50 px-3 py-2 text-sm text-green-700">{notice}</p>}
        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div>
          <label className="label">Email</label>
          <input className="input" type="email" {...register('email', { required: true })} />
        </div>
        <div>
          <label className="label">Password</label>
          <input className="input" type="password" {...register('password', { required: true })} />
        </div>
        <div className="text-right">
          <Link to="/forgot-password" className="text-sm font-medium text-brand-600">
            Forgot password?
          </Link>
        </div>
        <button className="btn-primary w-full" disabled={formState.isSubmitting}>
          {formState.isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        No account?{' '}
        <Link to="/register" className="font-medium text-brand-600">
          Create one
        </Link>
      </p>
    </AuthShell>
  );
}

export function AuthShell({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-1 text-center text-2xl font-bold text-brand-600">FlowSuite</h1>
        <h2 className="mb-6 text-center text-sm text-gray-500">{title}</h2>
        <div className="card">{children}</div>
      </div>
    </div>
  );
}
