import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import type { Role } from '../types';

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
}

const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard' },
  { to: '/projects', label: 'Projects' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/customers', label: 'Customers', roles: ['OWNER', 'ADMIN', 'MANAGER'] },
  { to: '/members', label: 'Members' },
  { to: '/billing', label: 'Billing', roles: ['OWNER', 'ADMIN'] },
  { to: '/audit', label: 'Audit Log', roles: ['OWNER', 'ADMIN'] },
];

export default function Layout() {
  const { user, hasRole, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = async () => {
    await logout();
    navigate('/login');
  };

  const visible = NAV.filter((item) => !item.roles || item.roles.some((r) => hasRole(r)));

  return (
    <div className="min-h-screen">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold text-brand-600">FlowSuite</span>
            <span className="badge bg-brand-50 text-brand-700">{user?.organizationName}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-gray-600">
              {user?.name} · <span className="font-medium">{user?.role}</span>
            </span>
            <button className="btn-secondary" onClick={onLogout}>
              Sign out
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-6xl gap-6 px-4 py-6">
        <nav className="w-48 shrink-0 space-y-1">
          {visible.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm font-medium ${
                  isActive ? 'bg-brand-50 text-brand-700' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
