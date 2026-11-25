import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../lib/api';
import type { Customer, Page } from '../types';

export default function CustomersPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ name: '', email: '', company: '' });
  const [error, setError] = useState<string | null>(null);

  const customers = useQuery({
    queryKey: ['customers'],
    queryFn: async () => (await api.get<Page<Customer>>('/api/v1/customers?size=50')).data,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['customers'] });

  const createMutation = useMutation({
    mutationFn: async () =>
      api.post('/api/v1/customers', {
        name: form.name,
        email: form.email || null,
        company: form.company || null,
      }),
    onSuccess: () => {
      setForm({ name: '', email: '', company: '' });
      setError(null);
      invalidate();
    },
    onError: (e) => setError(errorMessage(e, 'Could not create customer')),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/api/v1/customers/${id}`),
    onSuccess: invalidate,
  });

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Customers</h1>

      <form
        className="card space-y-3"
        onSubmit={(e) => {
          e.preventDefault();
          createMutation.mutate();
        }}
      >
        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div className="flex flex-col gap-3 sm:flex-row">
          <input
            className="input"
            placeholder="Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <input
            className="input"
            placeholder="Email"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
          <input
            className="input"
            placeholder="Company"
            value={form.company}
            onChange={(e) => setForm({ ...form, company: e.target.value })}
          />
          <button className="btn-primary shrink-0" disabled={createMutation.isPending}>
            Add customer
          </button>
        </div>
      </form>

      <div className="card p-0">
        {customers.isLoading ? (
          <p className="p-5 text-sm text-gray-500">Loading…</p>
        ) : customers.data?.content.length === 0 ? (
          <p className="p-5 text-sm text-gray-500">No customers yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-gray-200 text-left text-gray-500">
              <tr>
                <th className="px-5 py-3 font-medium">Name</th>
                <th className="px-5 py-3 font-medium">Email</th>
                <th className="px-5 py-3 font-medium">Company</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody>
              {customers.data?.content.map((c) => (
                <tr key={c.id} className="border-b border-gray-100 last:border-0">
                  <td className="px-5 py-3 font-medium">{c.name}</td>
                  <td className="px-5 py-3 text-gray-600">{c.email ?? '—'}</td>
                  <td className="px-5 py-3 text-gray-600">{c.company ?? '—'}</td>
                  <td className="px-5 py-3 text-right">
                    <button
                      className="text-xs font-medium text-red-600"
                      onClick={() => deleteMutation.mutate(c.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
