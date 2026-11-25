import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Member, Page, Project, Task, TaskStatus } from '../types';

const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function TasksPage() {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canManage = hasRole('OWNER', 'ADMIN', 'MANAGER');

  const [projectId, setProjectId] = useState('');
  const [title, setTitle] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [error, setError] = useState<string | null>(null);

  const tasks = useQuery({
    queryKey: ['tasks'],
    queryFn: async () => (await api.get<Page<Task>>('/api/v1/tasks?size=50')).data,
  });
  const projects = useQuery({
    queryKey: ['projects'],
    queryFn: async () => (await api.get<Page<Project>>('/api/v1/projects?size=50')).data,
  });
  const members = useQuery({
    queryKey: ['members'],
    queryFn: async () => (await api.get<Page<Member>>('/api/v1/members?size=50')).data,
  });

  const projectName = (id: string) => projects.data?.content.find((p) => p.id === id)?.name ?? '—';
  const memberName = (id?: string) =>
    id ? members.data?.content.find((m) => m.userId === id)?.name ?? 'Unknown' : 'Unassigned';

  const createMutation = useMutation({
    mutationFn: async () =>
      api.post('/api/v1/tasks', {
        projectId,
        title,
        assigneeId: assigneeId || null,
      }),
    onSuccess: () => {
      setTitle('');
      setAssigneeId('');
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (e) => setError(errorMessage(e, 'Could not create task')),
  });

  const statusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: TaskStatus }) =>
      api.patch(`/api/v1/tasks/${id}/status`, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
    onError: (e) => setError(errorMessage(e, 'Could not update status')),
  });

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Tasks</h1>

      {canManage && (
        <form
          className="card space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            if (projectId) createMutation.mutate();
          }}
        >
          {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          <div className="flex flex-col gap-3 sm:flex-row">
            <select className="input" value={projectId} onChange={(e) => setProjectId(e.target.value)} required>
              <option value="">Select project…</option>
              {projects.data?.content.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
            <input
              className="input"
              placeholder="Task title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
            <select className="input" value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
              <option value="">Unassigned</option>
              {members.data?.content.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.name}
                </option>
              ))}
            </select>
            <button className="btn-primary shrink-0" disabled={createMutation.isPending}>
              Add task
            </button>
          </div>
        </form>
      )}

      <div className="card p-0">
        {tasks.isLoading ? (
          <p className="p-5 text-sm text-gray-500">Loading…</p>
        ) : tasks.data?.content.length === 0 ? (
          <p className="p-5 text-sm text-gray-500">No tasks.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-gray-200 text-left text-gray-500">
              <tr>
                <th className="px-5 py-3 font-medium">Task</th>
                <th className="px-5 py-3 font-medium">Project</th>
                <th className="px-5 py-3 font-medium">Assignee</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {tasks.data?.content.map((t) => (
                <tr key={t.id} className="border-b border-gray-100 last:border-0">
                  <td className="px-5 py-3 font-medium">{t.title}</td>
                  <td className="px-5 py-3 text-gray-600">{projectName(t.projectId)}</td>
                  <td className="px-5 py-3 text-gray-600">{memberName(t.assigneeId)}</td>
                  <td className="px-5 py-3">
                    <select
                      className="rounded border border-gray-300 px-2 py-1 text-xs"
                      value={t.status}
                      onChange={(e) =>
                        statusMutation.mutate({ id: t.id, status: e.target.value as TaskStatus })
                      }
                    >
                      {STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
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
