import { jwtDecode } from 'jwt-decode';

const API_BASE = import.meta.env.VITE_API_BASE || '';

export interface JWTPayload {
    exp: number; // expiration (seconds since epoch)
}

export async function login(username: string, password: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    });
    if (!res.ok) {
        throw new Error('Login failed');
    }
    const { token } = (await res.json()) as { token: string };
    localStorage.setItem('jwt', token);
}

export function getToken(): string | null {
    return localStorage.getItem('jwt');
}

export function isLoggedIn(): boolean {
    const token = getToken();
    if (!token) return false;
    try {
        const { exp } = jwtDecode<JWTPayload>(token);
        return exp * 1000 > Date.now();
    } catch {
        return false;
    }
}

export function logout(): void {
    localStorage.removeItem('jwt');
}
