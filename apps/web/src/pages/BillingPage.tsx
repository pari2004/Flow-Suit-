import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Plan, PlanCode, Subscription } from '../types';

export default function BillingPage() {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canManage = hasRole('OWNER');
  const [notice, setNotice] = useState<string | null>(null);

  const subscription = useQuery({
    queryKey: ['subscription'],
    queryFn: async () => (await api.get<Subscription>('/api/v1/billing/subscription')).data,
  });

  const plans = useQuery({
    queryKey: ['plans'],
    queryFn: async () => (await api.get<Plan[]>('/api/v1/plans')).data,
  });

  const afterChange = () => {
    queryClient.invalidateQueries({ queryKey: ['subscription'] });
    queryClient.invalidateQueries({ queryKey: ['usage'] });
  };

  const simulateMutation = useMutation({
    mutationFn: async (planCode: PlanCode) =>
      api.post('/api/v1/billing/simulate-upgrade', { planCode }),
    onSuccess: () => {
      setNotice('Plan updated. Limits take effect immediately.');
      afterChange();
    },
    onError: (e) => setNotice(errorMessage(e, 'Could not change plan')),
  });

  const checkoutMutation = useMutation({
    mutationFn: async (planCode: PlanCode) =>
      (await api.post<{ checkoutUrl: string }>('/api/v1/billing/checkout', { planCode })).data,
    onSuccess: (data) => {
      window.location.href = data.checkoutUrl;
    },
    onError: (e) =>
      setNotice(errorMessage(e, 'Stripe checkout is unavailable. Configure Stripe keys to enable it.')),
  });

  const current = subscription.data?.planCode;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Billing</h1>
        {subscription.data && (
          <span className="badge bg-brand-50 text-brand-700">
            Current: {subscription.data.planName} · {subscription.data.status}
          </span>
        )}
      </div>

      {notice && <p className="rounded bg-brand-50 px-3 py-2 text-sm text-brand-700">{notice}</p>}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {plans.data?.map((plan) => (
          <div
            key={plan.code}
            className={`card ${plan.code === current ? 'ring-2 ring-brand-500' : ''}`}
          >
            <p className="text-sm text-gray-500">{plan.name}</p>
            <p className="mt-1 text-2xl font-semibold">
              ₹{plan.priceInr}
              <span className="text-sm font-normal text-gray-400">/mo</span>
            </p>
            <ul className="mt-3 space-y-1 text-sm text-gray-600">
              <li>{plan.seatLimit} seats</li>
              <li>{plan.projectLimit === null ? 'Unlimited' : plan.projectLimit} projects</li>
              <li>{plan.apiRequestLimit.toLocaleString()} API req/mo</li>
              <li>{plan.entitlements['ADVANCED_ANALYTICS'] ? 'Yes' : 'No'}: Advanced analytics</li>
            </ul>
            {canManage && plan.code !== current && (
              <div className="mt-4 space-y-2">
                <button
                  className="btn-primary w-full"
                  disabled={checkoutMutation.isPending || plan.code === 'FREE'}
                  onClick={() => checkoutMutation.mutate(plan.code)}
                >
                  Checkout with Stripe
                </button>
                <button
                  className="btn-secondary w-full"
                  disabled={simulateMutation.isPending}
                  onClick={() => simulateMutation.mutate(plan.code)}
                >
                  Switch (test mode)
                </button>
              </div>
            )}
            {plan.code === current && (
              <p className="mt-4 text-center text-xs font-medium text-brand-600">Current plan</p>
            )}
          </div>
        ))}
      </div>
      {!canManage && (
        <p className="text-sm text-gray-500">Only the organization Owner can change the plan.</p>
      )}
    </div>
  );
}
