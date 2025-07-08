import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { isLoggedIn } from '../services/auth';

interface Props {
    children: ReactNode;
}

export default function RequireAuth({ children }: Props) {
    const location = useLocation();
    if (!isLoggedIn()) {
        // Redirect to login, save current location
        return <Navigate to="/login" state={{ from: location }} replace />;
    }
    return <>{children}</>;
}
