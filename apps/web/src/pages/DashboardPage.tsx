import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Metric, UsageDashboard } from '../types';

function pct(metric: Metric): number {
  if (metric.unlimited || !metric.limit) return 0;
  return Math.min(100, Math.round((metric.used / metric.limit) * 100));
}

function MetricCard({ label, metric }: { label: string; metric: Metric }) {
  const percent = pct(metric);
  const over = !metric.unlimited && metric.limit != null && metric.used >= metric.limit;
  return (
    <div className="card">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold">
        {metric.used.toLocaleString()}
        <span className="text-base font-normal text-gray-400">
          {' '}
          / {metric.unlimited ? '∞' : metric.limit?.toLocaleString()}
        </span>
      </p>
      {!metric.unlimited && (
        <div className="mt-3 h-2 w-full rounded-full bg-gray-100">
          <div
            className={`h-2 rounded-full ${over ? 'bg-red-500' : 'bg-brand-500'}`}
            style={{ width: `${percent}%` }}
          />
        </div>
      )}
    </div>
  );
}

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const canView = hasRole('OWNER', 'ADMIN');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['usage'],
    queryFn: async () => (await api.get<UsageDashboard>('/api/v1/usage')).data,
    enabled: canView,
  });

  if (!canView) {
    return (
      <div className="card">
        <h1 className="text-xl font-semibold">Welcome back, {user?.name}</h1>
        <p className="mt-2 text-sm text-gray-600">
          You're signed in to <strong>{user?.organizationName}</strong> as {user?.role}. Use the
          navigation to view your projects and tasks.
        </p>
      </div>
    );
  }

  if (isLoading) return <p className="text-sm text-gray-500">Loading usage…</p>;
  if (isError || !data) return <p className="text-sm text-red-600">Could not load the usage dashboard.</p>;

  const chartData = [
    { name: 'Seats', value: pct(data.seats) },
    { name: 'Projects', value: data.projects.unlimited ? 0 : pct(data.projects) },
    { name: 'API', value: pct(data.apiRequests) },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Usage</h1>
        <span className="badge bg-brand-50 text-brand-700">
          {data.planName} · {data.subscriptionStatus}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <MetricCard label="Seats" metric={data.seats} />
        <MetricCard label="Projects" metric={data.projects} />
        <MetricCard label="API requests (this month)" metric={data.apiRequests} />
      </div>

      <div className="card">
        <p className="mb-4 text-sm font-medium text-gray-700">Percent of plan limit used</p>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" />
              <YAxis domain={[0, 100]} unit="%" />
              <Tooltip formatter={(v) => `${v}%`} />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {chartData.map((entry) => (
                  <Cell key={entry.name} fill={entry.value >= 100 ? '#ef4444' : '#6366f1'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-700">Advanced analytics</p>
          <p className="text-xs text-gray-500">Entitlement gated by plan</p>
        </div>
        <span
          className={`badge ${
            data.advancedAnalytics ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
          }`}
        >
          {data.advancedAnalytics ? 'Enabled' : 'Upgrade to unlock'}
        </span>
      </div>
    </div>
  );
}
