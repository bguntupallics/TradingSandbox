import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { fetchWithJwt, fetchPricesByPeriod } from './api';

// Mock the auth module
vi.mock('./auth', () => ({
    getToken: vi.fn(),
    logout: vi.fn(),
}));

import { getToken, logout } from './auth';

const mockGetToken = vi.mocked(getToken);
const mockLogout = vi.mocked(logout);

describe('api service', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        // Reset window.location
        Object.defineProperty(window, 'location', {
            value: { href: '' },
            writable: true,
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    // ── fetchWithJwt ─────────────────────────────────────────────────

    describe('fetchWithJwt', () => {
        it('adds Authorization header when token exists', async () => {
            mockGetToken.mockReturnValue('my-jwt-token');
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ data: 'test' }),
            });

            await fetchWithJwt('/api/test');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get('Authorization')).toBe('Bearer my-jwt-token');
        });

        it('does not add Authorization header when no token', async () => {
            mockGetToken.mockReturnValue(null);
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ data: 'test' }),
            });

            await fetchWithJwt('/api/test');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get('Authorization')).toBeNull();
        });

        it('sets Content-Type to application/json', async () => {
            mockGetToken.mockReturnValue(null);
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({}),
            });

            await fetchWithJwt('/api/test');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get('Content-Type')).toBe('application/json');
        });

        it('returns parsed JSON on success', async () => {
            mockGetToken.mockReturnValue('token');
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ result: 42 }),
            });

            const data = await fetchWithJwt<{ result: number }>('/api/test');
            expect(data.result).toBe(42);
        });

        it('calls logout and redirects on 401', async () => {
            mockGetToken.mockReturnValue('expired-token');
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 401,
                text: () => Promise.resolve('Unauthorized'),
            });

            await expect(fetchWithJwt('/api/test')).rejects.toThrow('Unauthorized');
            expect(mockLogout).toHaveBeenCalled();
            expect(window.location.href).toBe('/login');
        });

        it('throws on non-ok response with error text', async () => {
            mockGetToken.mockReturnValue('token');
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 500,
                statusText: 'Internal Server Error',
                text: () => Promise.resolve('Server error details'),
            });

            await expect(fetchWithJwt('/api/test')).rejects.toThrow('Server error details');
        });

        it('throws statusText when response text is empty', async () => {
            mockGetToken.mockReturnValue('token');
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 500,
                statusText: 'Internal Server Error',
                text: () => Promise.resolve(''),
            });

            await expect(fetchWithJwt('/api/test')).rejects.toThrow('Internal Server Error');
        });

        it('passes through additional options', async () => {
            mockGetToken.mockReturnValue('token');
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({}),
            });

            await fetchWithJwt('/api/test', { method: 'POST', body: '{}' });

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            expect(callArgs[1].method).toBe('POST');
            expect(callArgs[1].body).toBe('{}');
        });
    });

    // ── fetchPricesByPeriod ──────────────────────────────────────────

    describe('fetchPricesByPeriod', () => {
        it('calls correct URL for symbol and period', async () => {
            mockGetToken.mockReturnValue('token');
            const priceData = [
                { symbol: 'AAPL', timestamp: '2025-07-10', dateLabel: '7/10', closingPrice: 155 },
            ];
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve(priceData),
            });

            const result = await fetchPricesByPeriod('AAPL', '1M');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            expect(callArgs[0]).toContain('/api/prices/AAPL/period/1M');
            expect(result).toEqual(priceData);
        });

        it('returns array of PriceData objects', async () => {
            mockGetToken.mockReturnValue('token');
            const data = [
                { symbol: 'GOOG', timestamp: '2025-01-01', dateLabel: '1/1', closingPrice: 2800 },
                { symbol: 'GOOG', timestamp: '2025-01-02', dateLabel: '1/2', closingPrice: 2850 },
            ];
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve(data),
            });

            const result = await fetchPricesByPeriod('GOOG', '1W');
            expect(result).toHaveLength(2);
            expect(result[0].symbol).toBe('GOOG');
        });
    });
});
