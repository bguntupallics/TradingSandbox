import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AccountPage from './AccountPage';

vi.mock('../services/api', () => ({
    fetchApi: vi.fn(),
}));

const mockLogout = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
    useAuth: () => ({ logout: mockLogout }),
}));

import { fetchApi } from '../services/api';

const mockFetchApi = vi.mocked(fetchApi);

const mockAccount = {
    username: 'johndoe',
    email: 'john@example.com',
    firstName: 'John',
    lastName: 'Doe',
    cashBalance: 100000,
};

describe('AccountPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockLogout.mockResolvedValue(undefined);
        Object.defineProperty(window, 'location', {
            value: { href: '' },
            writable: true,
        });
    });

    it('renders Account heading', () => {
        mockFetchApi.mockResolvedValue(mockAccount);
        render(<AccountPage />);
        expect(screen.getByRole('heading', { name: /account/i })).toBeInTheDocument();
    });

    it('shows loading state initially', () => {
        mockFetchApi.mockReturnValue(new Promise(() => {}));
        render(<AccountPage />);
        expect(screen.getByText(/loading/i)).toBeInTheDocument();
    });

    it('displays account information on successful fetch', async () => {
        mockFetchApi.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
            expect(screen.getByText('john@example.com')).toBeInTheDocument();
            expect(screen.getByText('John Doe')).toBeInTheDocument();
        });
    });

    it('displays error on failed fetch', async () => {
        mockFetchApi.mockRejectedValue(new Error('Failed to load'));
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
        });
    });

    it('shows edit form when edit button is clicked', async () => {
        mockFetchApi.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        const editBtn = screen.getByRole('button', { name: '' });
        await userEvent.click(editBtn);

        expect(screen.getByDisplayValue('johndoe')).toBeInTheDocument();
        expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument();
        expect(screen.getByDisplayValue('John')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Doe')).toBeInTheDocument();
    });

    it('cancels editing and restores values', async () => {
        mockFetchApi.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        const editBtn = screen.getByRole('button', { name: '' });
        await userEvent.click(editBtn);

        const usernameInput = screen.getByDisplayValue('johndoe');
        await userEvent.clear(usernameInput);
        await userEvent.type(usernameInput, 'newname');

        await userEvent.click(screen.getByText('Cancel'));

        expect(screen.getByText('johndoe')).toBeInTheDocument();
    });

    it('submits updated profile', async () => {
        const updatedAccount = { ...mockAccount, firstName: 'Jane' };
        mockFetchApi
            .mockResolvedValueOnce(mockAccount)
            .mockResolvedValueOnce(updatedAccount);

        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        const editBtn = screen.getByRole('button', { name: '' });
        await userEvent.click(editBtn);

        await userEvent.click(screen.getByText('Save Changes'));

        await waitFor(() => {
            expect(mockFetchApi).toHaveBeenCalledWith('/api/account/update', expect.objectContaining({
                method: 'POST',
            }));
        });
    });

    it('calls logout and redirects on Log Out click', async () => {
        mockFetchApi.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        await userEvent.click(screen.getByText('Log Out'));

        await waitFor(() => {
            expect(mockLogout).toHaveBeenCalled();
            expect(window.location.href).toBe('/');
        });
    });

    it('calls fetchApi with correct path on mount', () => {
        mockFetchApi.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        expect(mockFetchApi).toHaveBeenCalledWith('/api/account');
    });
});
