import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StockSearchInput from './StockSearchInput';
import * as api from '../services/api';

// Mock the API module
vi.mock('../services/api', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../services/api')>();
    return {
        ...actual,
        searchStocks: vi.fn(),
        validateStock: vi.fn(),
    };
});

describe('StockSearchInput', () => {
    const defaultProps = {
        value: '',
        onChange: vi.fn(),
        onSearch: vi.fn(),
        onError: vi.fn(),
    };

    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers({ shouldAdvanceTime: true });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('renders input and search button', () => {
        render(<StockSearchInput {...defaultProps} />);

        expect(screen.getByPlaceholderText('e.g. NVDA')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument();
    });

    it('renders with custom placeholder', () => {
        render(<StockSearchInput {...defaultProps} placeholder="Custom placeholder" />);

        expect(screen.getByPlaceholderText('Custom placeholder')).toBeInTheDocument();
    });

    it('converts input to uppercase', async () => {
        const onChange = vi.fn();
        render(<StockSearchInput {...defaultProps} onChange={onChange} />);

        const input = screen.getByPlaceholderText('e.g. NVDA');
        await userEvent.type(input, 'a');

        // The component uppercases input before calling onChange
        expect(onChange).toHaveBeenCalledWith('A');
    });

    it('clears error on input change', async () => {
        const onError = vi.fn();
        render(<StockSearchInput {...defaultProps} onError={onError} />);

        const input = screen.getByPlaceholderText('e.g. NVDA');
        await userEvent.type(input, 'A');

        expect(onError).toHaveBeenCalledWith('');
    });

    it('shows error when searching with empty value', async () => {
        const onError = vi.fn();
        render(<StockSearchInput {...defaultProps} onError={onError} value="" />);

        const button = screen.getByRole('button', { name: /search/i });
        await userEvent.click(button);

        expect(onError).toHaveBeenCalledWith('Please enter a ticker symbol.');
    });

    it('validates stock before searching', async () => {
        const onSearch = vi.fn();
        const onError = vi.fn();
        const mockValidation = {
            valid: true,
            symbol: 'AAPL',
            name: 'Apple Inc.',
        };

        vi.mocked(api.validateStock).mockResolvedValue(mockValidation);

        render(
            <StockSearchInput
                {...defaultProps}
                value="AAPL"
                onSearch={onSearch}
                onError={onError}
            />
        );

        const button = screen.getByRole('button', { name: /search/i });
        await userEvent.click(button);

        await waitFor(() => {
            expect(api.validateStock).toHaveBeenCalledWith('AAPL');
        });

        await waitFor(() => {
            expect(onSearch).toHaveBeenCalledWith('AAPL', 'Apple Inc.');
        });
    });

    it('shows error for invalid stock', async () => {
        const onSearch = vi.fn();
        const onError = vi.fn();
        const mockValidation = {
            valid: false,
            error: "Stock symbol 'FAKE' not found",
        };

        vi.mocked(api.validateStock).mockResolvedValue(mockValidation);

        render(
            <StockSearchInput
                {...defaultProps}
                value="FAKE"
                onSearch={onSearch}
                onError={onError}
            />
        );

        const button = screen.getByRole('button', { name: /search/i });
        await userEvent.click(button);

        await waitFor(() => {
            expect(onError).toHaveBeenCalledWith("Stock symbol 'FAKE' not found");
        });

        expect(onSearch).not.toHaveBeenCalled();
    });

    it('displays suggestions dropdown after typing', async () => {
        const mockSuggestions = [
            { symbol: 'AAPL', name: 'Apple Inc.', exchange: 'NASDAQ' },
            { symbol: 'AMZN', name: 'Amazon.com Inc.', exchange: 'NASDAQ' },
        ];

        vi.mocked(api.searchStocks).mockResolvedValue(mockSuggestions);

        render(<StockSearchInput {...defaultProps} value="A" />);

        // Advance timers to trigger debounced search
        await act(async () => {
            vi.advanceTimersByTime(350);
        });

        await waitFor(() => {
            expect(api.searchStocks).toHaveBeenCalledWith('A', 8);
        });

        await waitFor(() => {
            expect(screen.getByText('AAPL')).toBeInTheDocument();
            expect(screen.getByText('Apple Inc.')).toBeInTheDocument();
        });
    });

    it('selects suggestion on click', async () => {
        const onChange = vi.fn();
        const onSearch = vi.fn();
        const mockSuggestions = [
            { symbol: 'AAPL', name: 'Apple Inc.', exchange: 'NASDAQ' },
        ];

        vi.mocked(api.searchStocks).mockResolvedValue(mockSuggestions);

        render(
            <StockSearchInput
                {...defaultProps}
                value="A"
                onChange={onChange}
                onSearch={onSearch}
            />
        );

        // Advance timers to trigger debounced search
        await act(async () => {
            vi.advanceTimersByTime(350);
        });

        await waitFor(() => {
            expect(screen.getByText('AAPL')).toBeInTheDocument();
        });

        await userEvent.click(screen.getByText('AAPL'));

        expect(onChange).toHaveBeenCalledWith('AAPL');
        expect(onSearch).toHaveBeenCalledWith('AAPL', 'Apple Inc.');
    });

    it('disables input and button when disabled', () => {
        render(<StockSearchInput {...defaultProps} disabled={true} />);

        expect(screen.getByPlaceholderText('e.g. NVDA')).toBeDisabled();
        expect(screen.getByRole('button')).toBeDisabled();
    });

    it('shows "Searching..." when disabled', () => {
        render(<StockSearchInput {...defaultProps} disabled={true} />);

        expect(screen.getByRole('button', { name: /searching/i })).toBeInTheDocument();
    });

    it('shows "Validating..." during validation', async () => {
        // Create a promise that we control
        let resolveValidation: (value: api.StockValidation) => void;
        const validationPromise = new Promise<api.StockValidation>((resolve) => {
            resolveValidation = resolve;
        });

        vi.mocked(api.validateStock).mockReturnValue(validationPromise);

        render(<StockSearchInput {...defaultProps} value="AAPL" />);

        const button = screen.getByRole('button', { name: /search/i });
        await userEvent.click(button);

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /validating/i })).toBeInTheDocument();
        });

        // Resolve the validation
        await act(async () => {
            resolveValidation!({ valid: true, symbol: 'AAPL', name: 'Apple Inc.' });
        });

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument();
        });
    });

    it('handles Enter key to submit search', async () => {
        const onSearch = vi.fn();
        vi.mocked(api.validateStock).mockResolvedValue({
            valid: true,
            symbol: 'AAPL',
            name: 'Apple Inc.',
        });

        render(<StockSearchInput {...defaultProps} value="AAPL" onSearch={onSearch} />);

        const input = screen.getByPlaceholderText('e.g. NVDA');
        await userEvent.type(input, '{Enter}');

        await waitFor(() => {
            expect(onSearch).toHaveBeenCalledWith('AAPL', 'Apple Inc.');
        });
    });

    it('handles keyboard navigation in suggestions', async () => {
        const mockSuggestions = [
            { symbol: 'AAPL', name: 'Apple Inc.', exchange: 'NASDAQ' },
            { symbol: 'AMZN', name: 'Amazon.com Inc.', exchange: 'NASDAQ' },
        ];

        vi.mocked(api.searchStocks).mockResolvedValue(mockSuggestions);

        render(<StockSearchInput {...defaultProps} value="A" />);

        // Advance timers to trigger debounced search
        await act(async () => {
            vi.advanceTimersByTime(350);
        });

        await waitFor(() => {
            expect(screen.getByText('AAPL')).toBeInTheDocument();
        });

        const input = screen.getByPlaceholderText('e.g. NVDA');

        // Press ArrowDown to select first item
        await userEvent.type(input, '{ArrowDown}');

        const firstItem = screen.getByText('AAPL').closest('li');
        expect(firstItem?.className).toContain('selected');

        // Press ArrowDown again to select second item
        await userEvent.type(input, '{ArrowDown}');

        const secondItem = screen.getByText('AMZN').closest('li');
        expect(secondItem?.className).toContain('selected');
    });

    it('closes suggestions on Escape key', async () => {
        const mockSuggestions = [
            { symbol: 'AAPL', name: 'Apple Inc.', exchange: 'NASDAQ' },
        ];

        vi.mocked(api.searchStocks).mockResolvedValue(mockSuggestions);

        render(<StockSearchInput {...defaultProps} value="A" />);

        // Advance timers to trigger debounced search
        await act(async () => {
            vi.advanceTimersByTime(350);
        });

        await waitFor(() => {
            expect(screen.getByText('AAPL')).toBeInTheDocument();
        });

        const input = screen.getByPlaceholderText('e.g. NVDA');
        await userEvent.type(input, '{Escape}');

        await waitFor(() => {
            expect(screen.queryByText('AAPL')).not.toBeInTheDocument();
        });
    });

    it('does not show suggestions for empty search results', async () => {
        vi.mocked(api.searchStocks).mockResolvedValue([]);

        render(<StockSearchInput {...defaultProps} value="XYZ" />);

        // Advance timers to trigger debounced search
        await act(async () => {
            vi.advanceTimersByTime(350);
        });

        await waitFor(() => {
            expect(api.searchStocks).toHaveBeenCalled();
        });

        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
});
