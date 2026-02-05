import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import Dashboard from './Dashboard';

vi.mock('../services/api', () => ({
    fetchWithJwt: vi.fn(),
}));

import { fetchWithJwt } from '../services/api';

const mockFetchWithJwt = vi.mocked(fetchWithJwt);

describe('Dashboard page', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders Dashboard heading', () => {
        mockFetchWithJwt.mockResolvedValue(10000);
        render(<Dashboard />);
        expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
    });

    it('shows loading state initially', () => {
        mockFetchWithJwt.mockReturnValue(new Promise(() => {})); // never resolves
        render(<Dashboard />);
        expect(screen.getByText(/loading/i)).toBeInTheDocument();
    });

    it('displays balance on successful fetch', async () => {
        mockFetchWithJwt.mockResolvedValue(100000);
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText(/buying power/i)).toBeInTheDocument();
        });

        expect(screen.getByText(/100,000/)).toBeInTheDocument();
    });

    it('displays error message on failed fetch', async () => {
        mockFetchWithJwt.mockRejectedValue(new Error('Network error'));
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText(/network error/i)).toBeInTheDocument();
        });
    });

    it('hides loading after fetch completes', async () => {
        mockFetchWithJwt.mockResolvedValue(50000);
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
        });
    });

    it('calls fetchWithJwt with correct path', () => {
        mockFetchWithJwt.mockResolvedValue(10000);
        render(<Dashboard />);

        expect(mockFetchWithJwt).toHaveBeenCalledWith('/api/account/balance');
    });

    it('does not show balance panel when balance is null (error state)', async () => {
        mockFetchWithJwt.mockRejectedValue(new Error('Fail'));
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.queryByText(/buying power/i)).not.toBeInTheDocument();
        });
    });
});
