import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface Props {
    children: ReactNode;
}

export default function RequireAuth({ children }: Props) {
    const { user, loading } = useAuth();
    const location = useLocation();

    if (loading) {
        return <div style={{ textAlign: 'center', marginTop: '4rem' }}>Loading...</div>;
    }

    if (!user) {
        return <Navigate to="/login" state={{ from: location.pathname }} replace />;
    }

    return <>{children}</>;
}
