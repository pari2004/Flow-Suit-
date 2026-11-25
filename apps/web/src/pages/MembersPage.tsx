import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Member, Page, Role } from '../types';

interface Invitation {
  id: string;
  email: string;
  role: Role;
  status: string;
  acceptToken?: string;
}

const ASSIGNABLE: Role[] = ['ADMIN', 'MANAGER', 'MEMBER'];

export default function MembersPage() {
  const queryClient = useQueryClient();
  const { hasRole, user } = useAuth();
  const canInvite = hasRole('OWNER', 'ADMIN');
  const canManageRoles = hasRole('OWNER');

  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('MEMBER');
  const [error, setError] = useState<string | null>(null);
  const [inviteLink, setInviteLink] = useState<string | null>(null);

  const members = useQuery({
    queryKey: ['members'],
    queryFn: async () => (await api.get<Page<Member>>('/api/v1/members?size=50')).data,
  });

  const invitations = useQuery({
    queryKey: ['invitations'],
    queryFn: async () =>
      (await api.get<Page<Invitation>>('/api/v1/members/invitations?size=50')).data,
    enabled: canInvite,
  });

  const inviteMutation = useMutation({
    mutationFn: async () =>
      (await api.post<Invitation>('/api/v1/members/invitations', { email, role })).data,
    onSuccess: (data) => {
      setEmail('');
      setError(null);
      if (data.acceptToken) {
        setInviteLink(`${window.location.origin}/accept-invite?token=${data.acceptToken}`);
      }
      queryClient.invalidateQueries({ queryKey: ['invitations'] });
    },
    onError: (e) => setError(errorMessage(e, 'Could not send invitation')),
  });

  const roleMutation = useMutation({
    mutationFn: async ({ id, newRole }: { id: string; newRole: Role }) =>
      api.patch(`/api/v1/members/${id}/role`, { role: newRole }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }),
    onError: (e) => setError(errorMessage(e, 'Could not change role')),
  });

  const removeMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/api/v1/members/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }),
    onError: (e) => setError(errorMessage(e, 'Could not remove member')),
  });

  const revokeMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/api/v1/members/invitations/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['invitations'] }),
  });

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">Members</h1>

      {canInvite && (
        <form
          className="card space-y-3"
          onSubmit={(e) => {
            e.preventDefault();
            inviteMutation.mutate();
          }}
        >
          {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          {inviteLink && (
            <div className="rounded bg-brand-50 px-3 py-2 text-xs text-brand-700">
              Invite link (share with the invitee):{' '}
              <code className="break-all">{inviteLink}</code>
            </div>
          )}
          <div className="flex flex-col gap-3 sm:flex-row">
            <input
              className="input"
              type="email"
              placeholder="invitee@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <select className="input sm:w-40" value={role} onChange={(e) => setRole(e.target.value as Role)}>
              {ASSIGNABLE.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
            <button className="btn-primary shrink-0" disabled={inviteMutation.isPending}>
              Invite
            </button>
          </div>
        </form>
      )}

      <div className="card p-0">
        <table className="w-full text-sm">
          <thead className="border-b border-gray-200 text-left text-gray-500">
            <tr>
              <th className="px-5 py-3 font-medium">Name</th>
              <th className="px-5 py-3 font-medium">Email</th>
              <th className="px-5 py-3 font-medium">Role</th>
              {canManageRoles && <th className="px-5 py-3" />}
            </tr>
          </thead>
          <tbody>
            {members.data?.content.map((m) => (
              <tr key={m.membershipId} className="border-b border-gray-100 last:border-0">
                <td className="px-5 py-3 font-medium">{m.name}</td>
                <td className="px-5 py-3 text-gray-600">{m.email}</td>
                <td className="px-5 py-3">
                  {canManageRoles && m.role !== 'OWNER' ? (
                    <select
                      className="rounded border border-gray-300 px-2 py-1 text-xs"
                      value={m.role}
                      onChange={(e) =>
                        roleMutation.mutate({ id: m.membershipId, newRole: e.target.value as Role })
                      }
                    >
                      {ASSIGNABLE.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span className="badge bg-gray-100 text-gray-600">{m.role}</span>
                  )}
                </td>
                {canManageRoles && (
                  <td className="px-5 py-3 text-right">
                    {m.role !== 'OWNER' && m.userId !== user?.id && (
                      <button
                        className="text-xs font-medium text-red-600"
                        onClick={() => removeMutation.mutate(m.membershipId)}
                      >
                        Remove
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {canInvite && invitations.data && invitations.data.content.length > 0 && (
        <div>
          <h2 className="mb-2 text-sm font-semibold text-gray-700">Pending invitations</h2>
          <div className="card p-0">
            <table className="w-full text-sm">
              <tbody>
                {invitations.data.content
                  .filter((i) => i.status === 'PENDING')
                  .map((i) => (
                    <tr key={i.id} className="border-b border-gray-100 last:border-0">
                      <td className="px-5 py-3">{i.email}</td>
                      <td className="px-5 py-3 text-gray-600">{i.role}</td>
                      <td className="px-5 py-3 text-right">
                        <button
                          className="text-xs font-medium text-red-600"
                          onClick={() => revokeMutation.mutate(i.id)}
                        >
                          Revoke
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
