import { Navigate } from 'react-router-dom';
import { isLoggedIn } from '../services/auth';

export default function HomeRedirect() {
    return isLoggedIn()
        ? <Navigate to="/dashboard" replace />
        : <Navigate to="/login" replace />;
}
