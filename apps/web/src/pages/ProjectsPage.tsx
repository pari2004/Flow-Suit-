import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Page, Project } from '../types';

export default function ProjectsPage() {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canManage = hasRole('OWNER', 'ADMIN', 'MANAGER');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);

  const projects = useQuery({
    queryKey: ['projects'],
    queryFn: async () => (await api.get<Page<Project>>('/api/v1/projects?size=50')).data,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['projects'] });

  const createMutation = useMutation({
    mutationFn: async () => (await api.post('/api/v1/projects', { name, description })).data,
    onSuccess: () => {
      setName('');
      setDescription('');
      setError(null);
      invalidate();
    },
    onError: (e) => setError(errorMessage(e, 'Could not create project')),
  });

  const archiveMutation = useMutation({
    mutationFn: async (p: Project) =>
      api.patch(`/api/v1/projects/${p.id}/${p.status === 'ARCHIVED' ? 'restore' : 'archive'}`),
    onSuccess: invalidate,
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/api/v1/projects/${id}`),
    onSuccess: invalidate,
  });

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Projects</h1>

      {canManage && (
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
              placeholder="Project name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
            <input
              className="input"
              placeholder="Description (optional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
            <button className="btn-primary shrink-0" disabled={createMutation.isPending}>
              Add project
            </button>
          </div>
        </form>
      )}

      <div className="card p-0">
        {projects.isLoading ? (
          <p className="p-5 text-sm text-gray-500">Loading…</p>
        ) : projects.data?.content.length === 0 ? (
          <p className="p-5 text-sm text-gray-500">No projects yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-gray-200 text-left text-gray-500">
              <tr>
                <th className="px-5 py-3 font-medium">Name</th>
                <th className="px-5 py-3 font-medium">Status</th>
                {canManage && <th className="px-5 py-3" />}
              </tr>
            </thead>
            <tbody>
              {projects.data?.content.map((p) => (
                <tr key={p.id} className="border-b border-gray-100 last:border-0">
                  <td className="px-5 py-3">
                    <div className="font-medium">{p.name}</div>
                    {p.description && <div className="text-xs text-gray-500">{p.description}</div>}
                  </td>
                  <td className="px-5 py-3">
                    <span
                      className={`badge ${
                        p.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                      }`}
                    >
                      {p.status}
                    </span>
                  </td>
                  {canManage && (
                    <td className="px-5 py-3 text-right">
                      <button
                        className="mr-2 text-xs font-medium text-brand-600"
                        onClick={() => archiveMutation.mutate(p)}
                      >
                        {p.status === 'ARCHIVED' ? 'Restore' : 'Archive'}
                      </button>
                      <button
                        className="text-xs font-medium text-red-600"
                        onClick={() => deleteMutation.mutate(p.id)}
                      >
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
