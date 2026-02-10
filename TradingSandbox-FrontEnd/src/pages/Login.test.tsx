import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';

vi.mock('../services/auth', () => ({
    login: vi.fn(),
    resendVerification: vi.fn(),
}));

const mockRefreshAuth = vi.fn();
const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
    useAuth: () => mockUseAuth(),
}));

import { login, resendVerification } from '../services/auth';

const mockLogin = vi.mocked(login);
const mockResendVerification = vi.mocked(resendVerification);

describe('Login page', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockRefreshAuth.mockResolvedValue(undefined);
        mockUseAuth.mockReturnValue({
            user: null,
            loading: false,
            refreshAuth: mockRefreshAuth,
            logout: vi.fn(),
        });
    });

    const renderLogin = (initialEntries = ['/login']) => {
        render(
            <MemoryRouter initialEntries={initialEntries}>
                <Login />
            </MemoryRouter>
        );
    };

    it('renders login heading', () => {
        renderLogin();
        expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
    });

    it('renders email and password inputs', () => {
        renderLogin();
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
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

    it('calls login function with email and password on form submit', async () => {
        mockLogin.mockResolvedValue(undefined);
        renderLogin();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        expect(mockLogin).toHaveBeenCalledWith('user@example.com', 'Password1');
    });

    it('calls refreshAuth after successful login', async () => {
        mockLogin.mockResolvedValue(undefined);
        renderLogin();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(mockRefreshAuth).toHaveBeenCalled();
        });
    });

    it('displays error message on failed login', async () => {
        mockLogin.mockRejectedValue(new Error('Invalid email or password'));
        renderLogin();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/password/i), 'wrongpass');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
        });
    });

    it('shows verification error and resend button when email not verified', async () => {
        mockLogin.mockRejectedValue(new Error('Email not verified'));
        renderLogin();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(screen.getByText(/email is not verified/i)).toBeInTheDocument();
            expect(screen.getByText(/resend verification email/i)).toBeInTheDocument();
        });
    });

    it('calls resendVerification when resend button is clicked', async () => {
        mockLogin.mockRejectedValue(new Error('Email not verified'));
        mockResendVerification.mockResolvedValue(undefined);
        renderLogin();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(screen.getByText(/resend verification email/i)).toBeInTheDocument();
        });

        await userEvent.click(screen.getByText(/resend verification email/i));

        await waitFor(() => {
            expect(mockResendVerification).toHaveBeenCalledWith('user@example.com');
            expect(screen.getByText(/verification email sent/i)).toBeInTheDocument();
        });
    });

    it('shows verified banner when URL has verified=true', () => {
        renderLogin(['/login?verified=true']);
        expect(screen.getByText(/email verified successfully/i)).toBeInTheDocument();
    });

    it('does not display error initially', () => {
        renderLogin();
        expect(screen.queryByText(/invalid email or password/i)).not.toBeInTheDocument();
    });

    it('disables submit button while submitting', async () => {
        mockLogin.mockImplementation(() => new Promise(() => {})); // never resolves
        renderLogin();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /log in/i }));

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /logging in/i })).toBeDisabled();
        });
    });
});
