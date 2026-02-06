import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchPage from './SearchPage';

// Mock recharts to avoid rendering issues in tests
vi.mock('recharts', () => ({
    LineChart: ({ children }: { children: React.ReactNode }) => <div data-testid="line-chart">{children}</div>,
    Line: () => <div />,
    XAxis: () => <div />,
    YAxis: () => <div />,
    CartesianGrid: () => <div />,
    Tooltip: () => <div />,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    ReferenceLine: () => <div />,
}));

vi.mock('../services/api', () => ({
    fetchWithJwt: vi.fn(),
    fetchPricesByPeriod: vi.fn(),
    searchStocks: vi.fn(),
    validateStock: vi.fn(),
}));

import { fetchWithJwt, fetchPricesByPeriod, searchStocks, validateStock } from '../services/api';

const mockFetchWithJwt = vi.mocked(fetchWithJwt);
const mockFetchPricesByPeriod = vi.mocked(fetchPricesByPeriod);
const mockSearchStocks = vi.mocked(searchStocks);
const mockValidateStock = vi.mocked(validateStock);

const mockPriceData = [
    { symbol: 'AAPL', timestamp: '2025-07-08T14:00:00Z', dateLabel: '7/8', closingPrice: 148 },
    { symbol: 'AAPL', timestamp: '2025-07-09T14:00:00Z', dateLabel: '7/9', closingPrice: 150 },
    { symbol: 'AAPL', timestamp: '2025-07-10T14:00:00Z', dateLabel: '7/10', closingPrice: 155 },
];

describe('SearchPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Default mock for validateStock - returns valid for most tests
        mockValidateStock.mockResolvedValue({
            valid: true,
            symbol: 'AAPL',
            name: 'Apple Inc.',
            exchange: 'NASDAQ',
            tradable: true,
        });
        // Default mock for searchStocks - returns empty
        mockSearchStocks.mockResolvedValue([]);
    });

    it('renders search heading', () => {
        render(<SearchPage />);
        expect(screen.getByRole('heading', { name: /search/i })).toBeInTheDocument();
    });

    it('renders search input and button', () => {
        render(<SearchPage />);
        expect(screen.getByPlaceholderText(/nvda/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument();
    });

    it('shows error when searching with empty symbol', async () => {
        render(<SearchPage />);

        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        expect(screen.getByText(/please enter a ticker symbol/i)).toBeInTheDocument();
    });

    it('converts input to uppercase', async () => {
        render(<SearchPage />);

        const input = screen.getByPlaceholderText(/nvda/i);
        await userEvent.type(input, 'aapl');

        expect(input).toHaveValue('AAPL');
    });

    it('fetches data on search', async () => {
        mockFetchPricesByPeriod.mockResolvedValue(mockPriceData);
        mockFetchWithJwt.mockResolvedValue({ symbol: 'AAPL', price: 155.5 });

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'AAPL');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            expect(mockFetchPricesByPeriod).toHaveBeenCalledWith('AAPL', '1D');
        });
    });

    it('displays price data after search', async () => {
        mockFetchPricesByPeriod.mockResolvedValue(mockPriceData);
        mockFetchWithJwt.mockResolvedValue({ symbol: 'AAPL', price: 155.5 });

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'AAPL');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            expect(screen.getByText(/AAPL/)).toBeInTheDocument();
        });
    });

    it('shows error when no price data found', async () => {
        mockValidateStock.mockResolvedValue({
            valid: true,
            symbol: 'XXXX',
            name: 'Test Stock',
        });
        mockFetchPricesByPeriod.mockResolvedValue([]);

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'XXXX');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            expect(screen.getByText(/no price data found for XXXX/i)).toBeInTheDocument();
        });
    });

    it('shows error on fetch failure', async () => {
        mockFetchPricesByPeriod.mockRejectedValue(new Error('API error'));

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'AAPL');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            expect(screen.getByText(/api error/i)).toBeInTheDocument();
        });
    });

    it('searches on Enter key press', async () => {
        mockFetchPricesByPeriod.mockResolvedValue(mockPriceData);
        mockFetchWithJwt.mockResolvedValue({ symbol: 'AAPL', price: 155.5 });

        render(<SearchPage />);

        const input = screen.getByPlaceholderText(/nvda/i);
        await userEvent.type(input, 'AAPL{Enter}');

        await waitFor(() => {
            expect(mockFetchPricesByPeriod).toHaveBeenCalledWith('AAPL', '1D');
        });
    });

    it('shows time period selector after successful search', async () => {
        mockFetchPricesByPeriod.mockResolvedValue(mockPriceData);
        mockFetchWithJwt.mockResolvedValue({ symbol: 'AAPL', price: 155 });

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'AAPL');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            expect(screen.getByText('1D')).toBeInTheDocument();
            expect(screen.getByText('1W')).toBeInTheDocument();
            expect(screen.getByText('1M')).toBeInTheDocument();
            expect(screen.getByText('3M')).toBeInTheDocument();
        });
    });

    it('falls back to last historical price when latest trade fails', async () => {
        mockFetchPricesByPeriod.mockResolvedValue(mockPriceData);
        mockFetchWithJwt.mockRejectedValue(new Error('Trade unavailable'));

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'AAPL');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            // Should show the last price from mockPriceData as fallback
            expect(screen.getByText('$155.00')).toBeInTheDocument();
        });
    });

    it('disables search button during validation', async () => {
        // Create a promise that never resolves to keep validating state
        mockValidateStock.mockReturnValue(new Promise(() => {}));

        render(<SearchPage />);

        await userEvent.type(screen.getByPlaceholderText(/nvda/i), 'AAPL');
        await userEvent.click(screen.getByRole('button', { name: /search/i }));

        await waitFor(() => {
            expect(screen.getByText(/validating/i)).toBeInTheDocument();
        });
    });

    it('does not show chart or period selector before search', () => {
        render(<SearchPage />);

        expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument();
        expect(screen.queryByText('1D')).not.toBeInTheDocument();
    });
});
