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

// Time period types and API function
export type TimePeriod = '1D' | '1W' | '1M' | '3M';

export interface PriceData {
    symbol: string;
    timestamp: string;
    dateLabel: string;
    closingPrice: number;
}

export interface StockSuggestion {
    symbol: string;
    name: string;
    exchange: string;
}

export interface StockSearchResult {
    suggestions: StockSuggestion[];
}

export interface StockValidation {
    valid: boolean;
    symbol?: string;
    name?: string;
    exchange?: string;
    tradable?: boolean;
    error?: string;
}

export async function fetchPricesByPeriod(
    symbol: string,
    period: TimePeriod
): Promise<PriceData[]> {
    return fetchWithJwt<PriceData[]>(`/api/prices/${symbol}/period/${period}`);
}

export async function searchStocks(
    query: string,
    limit: number = 10
): Promise<StockSuggestion[]> {
    if (!query || query.trim().length < 1) {
        return [];
    }
    const result = await fetchWithJwt<StockSearchResult>(
        `/api/prices/search/${encodeURIComponent(query.trim())}?limit=${limit}`
    );
    return result.suggestions || [];
}

export async function validateStock(symbol: string): Promise<StockValidation> {
    try {
        return await fetchWithJwt<StockValidation>(
            `/api/prices/validate/${encodeURIComponent(symbol.trim().toUpperCase())}`
        );
    } catch (err) {
        const message = (err as Error).message;
        return {
            valid: false,
            error: message || `Stock symbol '${symbol}' not found`,
        };
    }
}
