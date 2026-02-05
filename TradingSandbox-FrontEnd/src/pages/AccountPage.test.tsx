import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AccountPage from './AccountPage';

vi.mock('../services/api', () => ({
    fetchWithJwt: vi.fn(),
}));

vi.mock('../services/auth', () => ({
    logout: vi.fn(),
}));

import { fetchWithJwt } from '../services/api';
import { logout } from '../services/auth';

const mockFetchWithJwt = vi.mocked(fetchWithJwt);
const mockLogout = vi.mocked(logout);

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
        Object.defineProperty(window, 'location', {
            value: { href: '' },
            writable: true,
        });
    });

    it('renders Account heading', () => {
        mockFetchWithJwt.mockResolvedValue(mockAccount);
        render(<AccountPage />);
        expect(screen.getByRole('heading', { name: /account/i })).toBeInTheDocument();
    });

    it('shows loading state initially', () => {
        mockFetchWithJwt.mockReturnValue(new Promise(() => {}));
        render(<AccountPage />);
        expect(screen.getByText(/loading/i)).toBeInTheDocument();
    });

    it('displays account information on successful fetch', async () => {
        mockFetchWithJwt.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
            expect(screen.getByText('john@example.com')).toBeInTheDocument();
            expect(screen.getByText('John Doe')).toBeInTheDocument();
        });
    });

    it('displays error on failed fetch', async () => {
        mockFetchWithJwt.mockRejectedValue(new Error('Failed to load'));
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
        });
    });

    it('shows edit form when edit button is clicked', async () => {
        mockFetchWithJwt.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        // Click the edit button (it contains a Pencil icon)
        const editBtn = screen.getByRole('button', { name: '' });
        await userEvent.click(editBtn);

        // Should now show form inputs
        expect(screen.getByDisplayValue('johndoe')).toBeInTheDocument();
        expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument();
        expect(screen.getByDisplayValue('John')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Doe')).toBeInTheDocument();
    });

    it('cancels editing and restores values', async () => {
        mockFetchWithJwt.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        // Enter edit mode
        const editBtn = screen.getByRole('button', { name: '' });
        await userEvent.click(editBtn);

        // Modify a field
        const usernameInput = screen.getByDisplayValue('johndoe');
        await userEvent.clear(usernameInput);
        await userEvent.type(usernameInput, 'newname');

        // Click cancel
        await userEvent.click(screen.getByText('Cancel'));

        // Should show original values
        expect(screen.getByText('johndoe')).toBeInTheDocument();
    });

    it('submits updated profile', async () => {
        const updatedAccount = { ...mockAccount, firstName: 'Jane' };
        mockFetchWithJwt
            .mockResolvedValueOnce(mockAccount) // initial fetch
            .mockResolvedValueOnce(updatedAccount); // update fetch

        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        // Enter edit mode
        const editBtn = screen.getByRole('button', { name: '' });
        await userEvent.click(editBtn);

        // Click save
        await userEvent.click(screen.getByText('Save Changes'));

        await waitFor(() => {
            expect(mockFetchWithJwt).toHaveBeenCalledWith('/api/account/update', expect.objectContaining({
                method: 'POST',
            }));
        });
    });

    it('calls logout and redirects on Log Out click', async () => {
        mockFetchWithJwt.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        await waitFor(() => {
            expect(screen.getByText('johndoe')).toBeInTheDocument();
        });

        await userEvent.click(screen.getByText('Log Out'));

        expect(mockLogout).toHaveBeenCalled();
        expect(window.location.href).toBe('/');
    });

    it('calls fetchWithJwt with correct path on mount', () => {
        mockFetchWithJwt.mockResolvedValue(mockAccount);
        render(<AccountPage />);

        expect(mockFetchWithJwt).toHaveBeenCalledWith('/api/account');
    });
});
