import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { errorMessage } from '../lib/api';
import { AuthShell } from './LoginPage';

interface FormValues {
  name: string;
  email: string;
  password: string;
  organizationName: string;
}

export default function RegisterPage() {
  const { register: signUp, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<FormValues>();

  if (isAuthenticated) return <Navigate to="/" replace />;

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      await signUp(values);
      navigate('/');
    } catch (e) {
      setError(errorMessage(e, 'Could not create account'));
    }
  };

  return (
    <AuthShell title="Create your organization">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div>
          <label className="label">Your name</label>
          <input className="input" {...register('name', { required: true })} />
        </div>
        <div>
          <label className="label">Organization name</label>
          <input className="input" {...register('organizationName')} placeholder="Optional" />
        </div>
        <div>
          <label className="label">Email</label>
          <input className="input" type="email" {...register('email', { required: true })} />
        </div>
        <div>
          <label className="label">Password</label>
          <input
            className="input"
            type="password"
            {...register('password', { required: true, minLength: 8 })}
          />
          <p className="mt-1 text-xs text-gray-400">At least 8 characters.</p>
        </div>
        <button className="btn-primary w-full" disabled={formState.isSubmitting}>
          {formState.isSubmitting ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-brand-600">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
