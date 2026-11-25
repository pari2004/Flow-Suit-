import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { AuditLog, Page } from '../types';

export default function AuditLogsPage() {
  const [action, setAction] = useState('');
  const [page, setPage] = useState(0);

  const logs = useQuery({
    queryKey: ['audit', action, page],
    queryFn: async () =>
      (
        await api.get<Page<AuditLog>>('/api/v1/audit-logs', {
          params: { action: action || undefined, page, size: 20 },
        })
      ).data,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Audit log</h1>
        <input
          className="input w-56"
          placeholder="Filter by action…"
          value={action}
          onChange={(e) => {
            setPage(0);
            setAction(e.target.value.toUpperCase());
          }}
        />
      </div>

      <div className="card p-0">
        {logs.isLoading ? (
          <p className="p-5 text-sm text-gray-500">Loading…</p>
        ) : logs.data?.content.length === 0 ? (
          <p className="p-5 text-sm text-gray-500">No audit entries.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-gray-200 text-left text-gray-500">
              <tr>
                <th className="px-5 py-3 font-medium">Action</th>
                <th className="px-5 py-3 font-medium">Target</th>
                <th className="px-5 py-3 font-medium">When</th>
              </tr>
            </thead>
            <tbody>
              {logs.data?.content.map((log) => (
                <tr key={log.id} className="border-b border-gray-100 last:border-0">
                  <td className="px-5 py-3">
                    <span className="badge bg-gray-100 text-gray-700">{log.action}</span>
                  </td>
                  <td className="px-5 py-3 text-gray-600">
                    {log.targetType ?? '—'}
                    {log.targetId ? ` · ${log.targetId.slice(0, 8)}` : ''}
                  </td>
                  <td className="px-5 py-3 text-gray-500">
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {logs.data && (
        <div className="flex items-center justify-between text-sm text-gray-600">
          <span>
            Page {logs.data.page + 1} of {Math.max(1, logs.data.totalPages)}
          </span>
          <div className="flex gap-2">
            <button
              className="btn-secondary"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </button>
            <button className="btn-secondary" disabled={!logs.data.hasNext} onClick={() => setPage((p) => p + 1)}>
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
