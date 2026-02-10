import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../services/auth', () => ({
    fetchCurrentUser: vi.fn(),
    logout: vi.fn(),
}));

import { fetchCurrentUser, logout } from '../services/auth';

const mockFetchCurrentUser = vi.mocked(fetchCurrentUser);
const mockLogout = vi.mocked(logout);

const mockUser = {
    id: 1,
    username: 'johndoe',
    email: 'john@example.com',
    firstName: 'John',
    lastName: 'Doe',
    emailVerified: true,
    cashBalance: 100000,
    themePreference: 'dark',
};

// Test component to consume the context
function TestConsumer() {
    const { user, loading, refreshAuth, logout } = useAuth();

    return (
        <div>
            <div data-testid="loading">{String(loading)}</div>
            <div data-testid="user">{user ? JSON.stringify(user) : 'null'}</div>
            <button onClick={() => refreshAuth()}>Refresh</button>
            <button onClick={() => logout()}>Logout</button>
        </div>
    );
}

describe('AuthContext', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockLogout.mockResolvedValue(undefined);
    });

    it('starts in loading state and fetches current user on mount', async () => {
        mockFetchCurrentUser.mockResolvedValue(mockUser);

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        // Initially loading
        expect(screen.getByTestId('loading').textContent).toBe('true');

        // After fetch completes
        await waitFor(() => {
            expect(screen.getByTestId('loading').textContent).toBe('false');
            expect(screen.getByTestId('user').textContent).toContain('johndoe');
        });

        expect(mockFetchCurrentUser).toHaveBeenCalledTimes(1);
    });

    it('sets user to null when fetchCurrentUser returns null', async () => {
        mockFetchCurrentUser.mockResolvedValue(null);

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        await waitFor(() => {
            expect(screen.getByTestId('loading').textContent).toBe('false');
            expect(screen.getByTestId('user').textContent).toBe('null');
        });
    });

    it('refreshAuth re-fetches the current user', async () => {
        mockFetchCurrentUser
            .mockResolvedValueOnce(null)
            .mockResolvedValueOnce(mockUser);

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        // Wait for initial load (null user)
        await waitFor(() => {
            expect(screen.getByTestId('loading').textContent).toBe('false');
            expect(screen.getByTestId('user').textContent).toBe('null');
        });

        // Click refresh
        await act(async () => {
            await userEvent.click(screen.getByText('Refresh'));
        });

        await waitFor(() => {
            expect(screen.getByTestId('user').textContent).toContain('johndoe');
        });

        expect(mockFetchCurrentUser).toHaveBeenCalledTimes(2);
    });

    it('logout calls auth logout and sets user to null', async () => {
        mockFetchCurrentUser.mockResolvedValue(mockUser);

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        // Wait for user to be loaded
        await waitFor(() => {
            expect(screen.getByTestId('user').textContent).toContain('johndoe');
        });

        // Click logout
        await act(async () => {
            await userEvent.click(screen.getByText('Logout'));
        });

        await waitFor(() => {
            expect(mockLogout).toHaveBeenCalled();
            expect(screen.getByTestId('user').textContent).toBe('null');
        });
    });

    it('provides default values outside of provider', () => {
        render(<TestConsumer />);

        expect(screen.getByTestId('loading').textContent).toBe('true');
        expect(screen.getByTestId('user').textContent).toBe('null');
    });
});
