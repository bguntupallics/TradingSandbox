import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';

vi.mock('../services/auth', () => ({
    login: vi.fn(),
    isLoggedIn: vi.fn(),
}));

import { login, isLoggedIn } from '../services/auth';

const mockLogin = vi.mocked(login);
const mockIsLoggedIn = vi.mocked(isLoggedIn);

describe('Login page', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockIsLoggedIn.mockReturnValue(false);
    });

    const renderLogin = () => {
        render(
            <MemoryRouter initialEntries={['/login']}>
                <Login />
            </MemoryRouter>
        );
    };

    it('renders login heading', () => {
        renderLogin();
        expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
    });

    it('renders username and password inputs', () => {
        renderLogin();
        expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    });

    it('renders submit button', () => {
        renderLogin();
        expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();
    });

    it('renders link to register page', () => {
        renderLogin();
        expect(screen.getByText(/register here/i)).toBeInTheDocument();
    });

    it('calls login function on form submit', async () => {
        mockLogin.mockResolvedValue(undefined);
        renderLogin();

        await userEvent.type(screen.getByLabelText(/username/i), 'testuser');
        await userEvent.type(screen.getByLabelText(/password/i), 'password123');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        expect(mockLogin).toHaveBeenCalledWith('testuser', 'password123');
    });

    it('displays error message on failed login', async () => {
        mockLogin.mockRejectedValue(new Error('Login failed'));
        renderLogin();

        await userEvent.type(screen.getByLabelText(/username/i), 'testuser');
        await userEvent.type(screen.getByLabelText(/password/i), 'wrongpass');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(screen.getByText(/invalid username or password/i)).toBeInTheDocument();
        });
    });

    it('does not display error initially', () => {
        renderLogin();
        expect(screen.queryByText(/invalid username or password/i)).not.toBeInTheDocument();
    });

    it('clears error when form is resubmitted', async () => {
        mockLogin.mockRejectedValueOnce(new Error('Login failed'));
        renderLogin();

        await userEvent.type(screen.getByLabelText(/username/i), 'testuser');
        await userEvent.type(screen.getByLabelText(/password/i), 'wrong');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(screen.getByText(/invalid username or password/i)).toBeInTheDocument();
        });

        // Re-submit (error should be cleared before new attempt)
        mockLogin.mockResolvedValue(undefined);
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        // The error text should be cleared during the new submit attempt
        await waitFor(() => {
            expect(screen.queryByText(/invalid username or password/i)).not.toBeInTheDocument();
        });
    });
});
