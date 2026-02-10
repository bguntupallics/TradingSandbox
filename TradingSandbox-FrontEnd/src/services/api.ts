// src/services/api.ts
import { logout } from './auth';

const API_BASE = import.meta.env.VITE_API_BASE || '';

export async function fetchApi<T = never>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set('Content-Type', 'application/json');

    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers,
        credentials: 'include',
    });

    if (res.status === 401) {
        await logout();
        window.location.href = '/login';
        throw new Error('Unauthorized');
    }

    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || res.statusText);
    }

    return (await res.json()) as T;
}

/** @deprecated Use fetchApi instead */
export const fetchWithJwt = fetchApi;

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
    return fetchApi<PriceData[]>(`/api/prices/${symbol}/period/${period}`);
}

export async function searchStocks(
    query: string,
    limit: number = 10
): Promise<StockSuggestion[]> {
    if (!query || query.trim().length < 1) {
        return [];
    }
    const result = await fetchApi<StockSearchResult>(
        `/api/prices/search/${encodeURIComponent(query.trim())}?limit=${limit}`
    );
    return result.suggestions || [];
}

export async function validateStock(symbol: string): Promise<StockValidation> {
    try {
        return await fetchApi<StockValidation>(
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

// --- Trading Types ---

export interface TradeRequest {
    symbol: string;
    quantity: number;
    type: 'BUY' | 'SELL';
}

export interface TradeResult {
    tradeId: number;
    symbol: string;
    type: string;
    quantity: number;
    pricePerShare: number;
    totalCost: number;
    remainingCashBalance: number;
    executedAt: string;
}

export interface HoldingData {
    symbol: string;
    quantity: number;
    averageCost: number;
    currentPrice: number;
    marketValue: number;
    totalGainLoss: number;
    totalGainLossPercent: number;
}

export interface PortfolioData {
    cashBalance: number;
    holdingsValue: number;
    totalPortfolioValue: number;
    totalGainLoss: number;
    holdings: HoldingData[];
}

export interface TradeHistoryItem {
    id: number;
    symbol: string;
    type: string;
    quantity: number;
    pricePerShare: number;
    totalCost: number;
    executedAt: string;
}

export interface MarketStatus {
    open: boolean;
    nextOpen: string;
    nextClose: string;
}

// --- Trading API Functions ---

export async function executeTrade(request: TradeRequest): Promise<TradeResult> {
    return fetchApi<TradeResult>('/api/trade/execute', {
        method: 'POST',
        body: JSON.stringify(request),
    });
}

export async function fetchPortfolio(): Promise<PortfolioData> {
    return fetchApi<PortfolioData>('/api/trade/portfolio');
}

export async function fetchTradeHistory(): Promise<TradeHistoryItem[]> {
    return fetchApi<TradeHistoryItem[]>('/api/trade/history');
}

export async function fetchMarketStatus(): Promise<MarketStatus> {
    return fetchApi<MarketStatus>('/api/prices/market-status');
}
