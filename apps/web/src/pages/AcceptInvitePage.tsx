import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { errorMessage } from '../lib/api';
import { AuthShell } from './LoginPage';

interface FormValues {
  name: string;
  password: string;
}

export default function AcceptInvitePage() {
  const { acceptInvitation, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<FormValues>();

  if (isAuthenticated) return <Navigate to="/" replace />;

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      await acceptInvitation({ token, name: values.name, password: values.password });
      navigate('/');
    } catch (e) {
      setError(errorMessage(e, 'Could not accept invitation'));
    }
  };

  return (
    <AuthShell title="Accept your invitation">
      {!token ? (
        <p className="text-sm text-red-700">Missing invitation token in the link.</p>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          <div>
            <label className="label">Your name</label>
            <input className="input" {...register('name', { required: true })} />
          </div>
          <div>
            <label className="label">Choose a password</label>
            <input
              className="input"
              type="password"
              {...register('password', { required: true, minLength: 8 })}
            />
          </div>
          <button className="btn-primary w-full" disabled={formState.isSubmitting}>
            {formState.isSubmitting ? 'Joining…' : 'Join organization'}
          </button>
        </form>
      )}
    </AuthShell>
  );
}
