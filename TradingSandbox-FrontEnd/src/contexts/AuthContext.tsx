import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { fetchCurrentUser, type AuthUser, logout as authLogout } from '../services/auth';

interface AuthContextType {
    user: AuthUser | null;
    loading: boolean;
    refreshAuth: () => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
    user: null,
    loading: true,
    refreshAuth: async () => {},
    logout: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthUser | null>(null);
    const [loading, setLoading] = useState(true);

    const refreshAuth = useCallback(async () => {
        setLoading(true);
        const currentUser = await fetchCurrentUser();
        setUser(currentUser);
        setLoading(false);
    }, []);

    const logout = useCallback(async () => {
        await authLogout();
        setUser(null);
    }, []);

    useEffect(() => {
        refreshAuth();
    }, [refreshAuth]);

    return (
        <AuthContext.Provider value={{ user, loading, refreshAuth, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}
