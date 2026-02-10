import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { fetchApi, fetchWithJwt, fetchPricesByPeriod } from './api';

// Mock the auth module
vi.mock('./auth', () => ({
    logout: vi.fn(),
}));

import { logout } from './auth';

const mockLogout = vi.mocked(logout);

describe('api service', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        mockLogout.mockResolvedValue(undefined);
        Object.defineProperty(window, 'location', {
            value: { href: '' },
            writable: true,
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    // ── fetchApi ──────────────────────────────────────────────────────

    describe('fetchApi', () => {
        it('sends requests with credentials include', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ data: 'test' }),
            });

            await fetchApi('/api/test');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            expect(callArgs[1].credentials).toBe('include');
        });

        it('sets Content-Type to application/json', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({}),
            });

            await fetchApi('/api/test');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get('Content-Type')).toBe('application/json');
        });

        it('does not include Authorization header', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({}),
            });

            await fetchApi('/api/test');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get('Authorization')).toBeNull();
        });

        it('returns parsed JSON on success', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ result: 42 }),
            });

            const data = await fetchApi<{ result: number }>('/api/test');
            expect(data.result).toBe(42);
        });

        it('calls logout and redirects on 401', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 401,
                text: () => Promise.resolve('Unauthorized'),
            });

            await expect(fetchApi('/api/test')).rejects.toThrow('Unauthorized');
            expect(mockLogout).toHaveBeenCalled();
            expect(window.location.href).toBe('/login');
        });

        it('throws on non-ok response with error text', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 500,
                statusText: 'Internal Server Error',
                text: () => Promise.resolve('Server error details'),
            });

            await expect(fetchApi('/api/test')).rejects.toThrow('Server error details');
        });

        it('throws statusText when response text is empty', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: false,
                status: 500,
                statusText: 'Internal Server Error',
                text: () => Promise.resolve(''),
            });

            await expect(fetchApi('/api/test')).rejects.toThrow('Internal Server Error');
        });

        it('passes through additional options', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({}),
            });

            await fetchApi('/api/test', { method: 'POST', body: '{}' });

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            expect(callArgs[1].method).toBe('POST');
            expect(callArgs[1].body).toBe('{}');
        });
    });

    // ── fetchWithJwt (deprecated alias) ──────────────────────────────

    describe('fetchWithJwt (deprecated alias)', () => {
        it('is the same function as fetchApi', () => {
            expect(fetchWithJwt).toBe(fetchApi);
        });
    });

    // ── fetchPricesByPeriod ──────────────────────────────────────────

    describe('fetchPricesByPeriod', () => {
        it('calls correct URL for symbol and period', async () => {
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

        it('uses credentials include', async () => {
            global.fetch = vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve([]),
            });

            await fetchPricesByPeriod('AAPL', '1D');

            const callArgs = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
            expect(callArgs[1].credentials).toBe('include');
        });
    });
});
