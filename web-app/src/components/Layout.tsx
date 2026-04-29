import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  Home, Users, CreditCard, Calendar, BarChart2, Settings,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { getInitials } from '../utils/format';
import RadafiqLogo from './RadafiqLogo';

const NAV_ITEMS = [
  { path: '/dashboard', label: 'Home', icon: Home },
  { path: '/customers', label: 'Customers', icon: Users },
  { path: '/accounts', label: 'Accounts', icon: CreditCard },
  { path: '/emi', label: 'EMI', icon: Calendar },
  { path: '/analytics', label: 'Analytics', icon: BarChart2 },
  { path: '/settings', label: 'Settings', icon: Settings },
];

export default function Layout({ children }: { children: React.ReactNode }) {
  const { profile } = useApp();
  const location = useLocation();

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="nav-logo">
          <RadafiqLogo size={40} />
          <div>
            <div className="nav-logo-text">Radafiq</div>
            <div className="nav-logo-sub">Finance Manager</div>
          </div>
        </div>

        <nav style={{ flex: 1 }}>
          {NAV_ITEMS.map(({ path, label, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>

        {profile && (
          <div style={{ padding: '1rem 1.25rem', borderTop: '1px solid var(--outline)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div className="avatar" style={{ width: 36, height: 36, fontSize: '0.75rem', flexShrink: 0 }}>
                {profile.photoUrl ? (
                  <img
                    src={profile.photoUrl}
                    alt=""
                    referrerPolicy="no-referrer"
                    style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
                  />
                ) : (
                  getInitials(profile.displayName)
                )}
              </div>
              <div style={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {profile.displayName}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {profile.businessName}
                </div>
              </div>
            </div>
          </div>
        )}
      </aside>

      {/* Main content */}
      <main className="main-content">
        <div className="radafiq-bg">
          <div className="fade-in">
            {children}
          </div>
        </div>
      </main>

      {/* Bottom nav (mobile) */}
      <nav className="bottom-nav">
        {NAV_ITEMS.slice(0, 5).map(({ path, label, icon: Icon }) => {
          const isActive = location.pathname.startsWith(path);
          return (
            <NavLink
              key={path}
              to={path}
              className={`bottom-nav-item${isActive ? ' active' : ''}`}
            >
              <Icon size={20} />
              {label}
            </NavLink>
          );
        })}
      </nav>
    </div>
  );
}
