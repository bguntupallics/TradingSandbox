import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { login, getToken, isLoggedIn, logout } from './auth';

// Mock jwt-decode
vi.mock('jwt-decode', () => ({
    jwtDecode: vi.fn(),
}));

import { jwtDecode } from 'jwt-decode';

const mockJwtDecode = vi.mocked(jwtDecode);

describe('auth service', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.restoreAllMocks();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    // ── getToken ─────────────────────────────────────────────────────

    describe('getToken', () => {
        it('returns null when no token stored', () => {
            expect(getToken()).toBeNull();
        });

        it('returns stored token', () => {
            localStorage.setItem('jwt', 'test-token');
            expect(getToken()).toBe('test-token');
        });
    });

    // ── isLoggedIn ───────────────────────────────────────────────────

    describe('isLoggedIn', () => {
        it('returns false when no token stored', () => {
            expect(isLoggedIn()).toBe(false);
        });

        it('returns true when token is not expired', () => {
            localStorage.setItem('jwt', 'valid-token');
            // exp is in seconds, set to future
            mockJwtDecode.mockReturnValue({ exp: Math.floor(Date.now() / 1000) + 3600 });

            expect(isLoggedIn()).toBe(true);
        });

        it('returns false when token is expired', () => {
            localStorage.setItem('jwt', 'expired-token');
            // exp is in the past
            mockJwtDecode.mockReturnValue({ exp: Math.floor(Date.now() / 1000) - 3600 });

            expect(isLoggedIn()).toBe(false);
        });

        it('returns false when jwtDecode throws', () => {
            localStorage.setItem('jwt', 'malformed-token');
            mockJwtDecode.mockImplementation(() => {
                throw new Error('Invalid token');
            });

            expect(isLoggedIn()).toBe(false);
        });
    });

    // ── logout ───────────────────────────────────────────────────────

    describe('logout', () => {
        it('removes jwt from localStorage', () => {
            localStorage.setItem('jwt', 'some-token');
            logout();
            expect(localStorage.getItem('jwt')).toBeNull();
        });

        it('does not throw when no token stored', () => {
            expect(() => logout()).not.toThrow();
        });
    });

    // ── login ────────────────────────────────────────────────────────

    describe('login', () => {
        it('stores token on successful login', async () => {
            const mockResponse = {
                ok: true,
                json: () => Promise.resolve({ token: 'jwt-from-server' }),
            };
            global.fetch = vi.fn().mockResolvedValue(mockResponse);

            await login('testuser', 'password123');

            expect(localStorage.getItem('jwt')).toBe('jwt-from-server');
            expect(fetch).toHaveBeenCalledWith(
                expect.stringContaining('/api/auth/login'),
                expect.objectContaining({
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: 'testuser', password: 'password123' }),
                })
            );
        });

        it('throws on failed login', async () => {
            const mockResponse = {
                ok: false,
                status: 401,
            };
            global.fetch = vi.fn().mockResolvedValue(mockResponse);

            await expect(login('testuser', 'wrongpass')).rejects.toThrow('Login failed');
            expect(localStorage.getItem('jwt')).toBeNull();
        });

        it('sends correct request body', async () => {
            const mockResponse = {
                ok: true,
                json: () => Promise.resolve({ token: 'token123' }),
            };
            global.fetch = vi.fn().mockResolvedValue(mockResponse);

            await login('myuser', 'mypass');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            const body = JSON.parse(callArgs[1].body);
            expect(body).toEqual({ username: 'myuser', password: 'mypass' });
        });
    });
});
