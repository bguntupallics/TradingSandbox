const API_BASE = import.meta.env.VITE_API_BASE || '';

export interface AuthUser {
    id: number;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    emailVerified: boolean;
    cashBalance: number;
    themePreference: string;
}

/**
 * Login with email and password.
 * The server sets an httpOnly cookie — we don't handle the token.
 */
export async function login(email: string, password: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password }),
    });
    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || 'Login failed');
    }
}

/**
 * Register a new account. Does NOT auto-login — user must verify email first.
 */
export async function register(
    email: string,
    password: string,
    firstName: string,
    lastName: string,
): Promise<void> {
    const res = await fetch(`${API_BASE}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email, password, firstName, lastName }),
    });
    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        const msg = body.error || body.errors?.join(', ') || 'Registration failed';
        throw new Error(msg);
    }
}

/**
 * Check if the user is currently authenticated by calling /api/auth/me.
 * Returns user data if authenticated, null otherwise.
 */
export async function fetchCurrentUser(): Promise<AuthUser | null> {
    try {
        const res = await fetch(`${API_BASE}/api/auth/me`, {
            credentials: 'include',
        });
        if (!res.ok) return null;
        return (await res.json()) as AuthUser;
    } catch {
        return null;
    }
}

/**
 * Logout: call the server to clear the httpOnly cookie.
 */
export async function logout(): Promise<void> {
    await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
    });
}

/**
 * Resend the email verification link.
 */
export async function resendVerification(email: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ email }),
    });
    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || 'Failed to resend verification email');
    }
}
