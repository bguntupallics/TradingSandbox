import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function HomeRedirect() {
    const { user, loading } = useAuth();

    if (loading) {
        return <div style={{ textAlign: 'center', marginTop: '4rem' }}>Loading...</div>;
    }

    return user
        ? <Navigate to="/dashboard" replace />
        : <Navigate to="/login" replace />;
}
