import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';

vi.mock('../services/api', () => ({
    fetchPortfolio: vi.fn(),
    fetchTradeHistory: vi.fn(),
}));

import { fetchPortfolio, fetchTradeHistory } from '../services/api';

const mockFetchPortfolio = vi.mocked(fetchPortfolio);
const mockFetchTradeHistory = vi.mocked(fetchTradeHistory);

const renderDashboard = () =>
    render(
        <MemoryRouter>
            <Dashboard />
        </MemoryRouter>
    );

describe('Dashboard page', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders Dashboard heading', async () => {
        mockFetchPortfolio.mockResolvedValue({
            cashBalance: 100000,
            holdingsValue: 0,
            totalPortfolioValue: 100000,
            totalGainLoss: 0,
            holdings: [],
        });
        mockFetchTradeHistory.mockResolvedValue([]);

        renderDashboard();
        expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
    });

    it('shows loading state initially', () => {
        mockFetchPortfolio.mockReturnValue(new Promise(() => {}));
        mockFetchTradeHistory.mockReturnValue(new Promise(() => {}));

        renderDashboard();
        expect(screen.getByText(/loading/i)).toBeInTheDocument();
    });

    it('displays portfolio value on successful fetch', async () => {
        mockFetchPortfolio.mockResolvedValue({
            cashBalance: 50000,
            holdingsValue: 50000,
            totalPortfolioValue: 100000,
            totalGainLoss: 5000,
            holdings: [
                {
                    symbol: 'AAPL',
                    quantity: 10,
                    averageCost: 175,
                    currentPrice: 180,
                    marketValue: 1800,
                    totalGainLoss: 50,
                    totalGainLossPercent: 2.86,
                },
            ],
        });
        mockFetchTradeHistory.mockResolvedValue([]);

        renderDashboard();

        await waitFor(() => {
            expect(screen.getByText(/\$100,000\.00/)).toBeInTheDocument();
        });
    });

    it('displays buying power', async () => {
        mockFetchPortfolio.mockResolvedValue({
            cashBalance: 75000,
            holdingsValue: 25000,
            totalPortfolioValue: 100000,
            totalGainLoss: 0,
            holdings: [],
        });
        mockFetchTradeHistory.mockResolvedValue([]);

        renderDashboard();

        await waitFor(() => {
            expect(screen.getByText(/buying power/i)).toBeInTheDocument();
            expect(screen.getByText(/\$75,000\.00/)).toBeInTheDocument();
        });
    });

    it('displays error message on failed fetch', async () => {
        mockFetchPortfolio.mockRejectedValue(new Error('Network error'));
        mockFetchTradeHistory.mockRejectedValue(new Error('Network error'));

        renderDashboard();

        await waitFor(() => {
            expect(screen.getByText(/network error/i)).toBeInTheDocument();
        });
    });

    it('shows empty state when no holdings', async () => {
        mockFetchPortfolio.mockResolvedValue({
            cashBalance: 100000,
            holdingsValue: 0,
            totalPortfolioValue: 100000,
            totalGainLoss: 0,
            holdings: [],
        });
        mockFetchTradeHistory.mockResolvedValue([]);

        renderDashboard();

        await waitFor(() => {
            expect(screen.getByText(/no holdings yet/i)).toBeInTheDocument();
        });
    });

    it('displays holdings list', async () => {
        mockFetchPortfolio.mockResolvedValue({
            cashBalance: 50000,
            holdingsValue: 50000,
            totalPortfolioValue: 100000,
            totalGainLoss: 2000,
            holdings: [
                {
                    symbol: 'AAPL',
                    quantity: 10,
                    averageCost: 175,
                    currentPrice: 195,
                    marketValue: 1950,
                    totalGainLoss: 200,
                    totalGainLossPercent: 11.43,
                },
            ],
        });
        mockFetchTradeHistory.mockResolvedValue([]);

        renderDashboard();

        await waitFor(() => {
            expect(screen.getByText('AAPL')).toBeInTheDocument();
        });
    });
});
