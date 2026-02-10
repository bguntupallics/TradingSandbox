import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { login, register, fetchCurrentUser, logout, resendVerification } from './auth';

describe('auth service', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    // ── login ────────────────────────────────────────────────────────

    describe('login', () => {
        it('sends email and password with credentials include', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });

            await login('user@example.com', 'Password1');

            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/login'),
                expect.objectContaining({
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ email: 'user@example.com', password: 'Password1' }),
                })
            );
        });

        it('resolves on successful login', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });
            await expect(login('user@example.com', 'Password1')).resolves.toBeUndefined();
        });

        it('throws error message from server on failure', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.resolve({ error: 'Email not verified' }),
            });

            await expect(login('user@example.com', 'Password1')).rejects.toThrow('Email not verified');
        });

        it('throws generic message when server returns no error field', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.resolve({}),
            });

            await expect(login('user@example.com', 'Password1')).rejects.toThrow('Login failed');
        });

        it('throws generic message when JSON parsing fails', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.reject(new Error('invalid json')),
            });

            await expect(login('user@example.com', 'Password1')).rejects.toThrow('Login failed');
        });
    });

    // ── register ─────────────────────────────────────────────────────

    describe('register', () => {
        it('sends email, password, firstName, lastName with credentials include', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });

            await register('user@example.com', 'Password1', 'John', 'Doe');

            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/register'),
                expect.objectContaining({
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({
                        email: 'user@example.com',
                        password: 'Password1',
                        firstName: 'John',
                        lastName: 'Doe',
                    }),
                })
            );
        });

        it('resolves on successful registration', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });
            await expect(register('u@e.com', 'Pass1234', 'A', 'B')).resolves.toBeUndefined();
        });

        it('throws server error message on failure', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.resolve({ error: 'Email already in use' }),
            });

            await expect(register('u@e.com', 'Pass1234', 'A', 'B')).rejects.toThrow('Email already in use');
        });

        it('throws joined validation errors', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.resolve({ errors: ['Invalid email', 'Password too short'] }),
            });

            await expect(register('bad', 'x', 'A', 'B')).rejects.toThrow('Invalid email, Password too short');
        });

        it('throws generic message when no error info', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.reject(new Error('bad json')),
            });

            await expect(register('u@e.com', 'Pass1234', 'A', 'B')).rejects.toThrow('Registration failed');
        });
    });

    // ── fetchCurrentUser ─────────────────────────────────────────────

    describe('fetchCurrentUser', () => {
        it('returns user data on success', async () => {
            const userData = {
                id: 1,
                username: 'johndoe',
                email: 'john@example.com',
                firstName: 'John',
                lastName: 'Doe',
                emailVerified: true,
                cashBalance: 100000,
                themePreference: 'dark',
            };
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                json: () => Promise.resolve(userData),
            });

            const result = await fetchCurrentUser();

            expect(result).toEqual(userData);
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/me'),
                expect.objectContaining({ credentials: 'include' })
            );
        });

        it('returns null on non-ok response', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 401,
            });

            const result = await fetchCurrentUser();
            expect(result).toBeNull();
        });

        it('returns null when fetch throws', async () => {
            global.fetch = vi.fn().mockRejectedValue(new Error('Network error'));

            const result = await fetchCurrentUser();
            expect(result).toBeNull();
        });
    });

    // ── logout ───────────────────────────────────────────────────────

    describe('logout', () => {
        it('calls logout endpoint with credentials include', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });

            await logout();

            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/logout'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include',
                })
            );
        });

        it('does not throw even if server returns error', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 500 });
            await expect(logout()).resolves.toBeUndefined();
        });
    });

    // ── resendVerification ───────────────────────────────────────────

    describe('resendVerification', () => {
        it('sends email in body with credentials include', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });

            await resendVerification('user@example.com');

            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/resend-verification'),
                expect.objectContaining({
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ email: 'user@example.com' }),
                })
            );
        });

        it('resolves on success', async () => {
            global.fetch = vi.fn().mockResolvedValue({ ok: true });
            await expect(resendVerification('u@e.com')).resolves.toBeUndefined();
        });

        it('throws server error message on failure', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.resolve({ error: 'No account found' }),
            });

            await expect(resendVerification('u@e.com')).rejects.toThrow('No account found');
        });

        it('throws generic message when no error field', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                json: () => Promise.reject(new Error('bad json')),
            });

            await expect(resendVerification('u@e.com')).rejects.toThrow('Failed to resend verification email');
        });
    });
});
