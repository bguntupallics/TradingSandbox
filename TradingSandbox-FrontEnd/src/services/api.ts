// src/services/api.ts
import { getToken, logout } from './auth';

const API_BASE = import.meta.env.VITE_API_BASE || '';

export async function fetchWithJwt<T = never>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    // Build a Headers object (no more implicit any!)
    const headers = new Headers(options.headers);
    headers.set('Content-Type', 'application/json');

    const token = getToken();
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers,              // pass the Headers instance
    });

    if (res.status === 401) {
        logout();
        window.location.href = '/login';
        throw new Error('Unauthorized');
    }

    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || res.statusText);
    }

    return (await res.json()) as T;
}
