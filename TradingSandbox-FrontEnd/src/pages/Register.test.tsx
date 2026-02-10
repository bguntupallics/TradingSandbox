import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Register from './Register';

vi.mock('../services/auth', () => ({
    register: vi.fn(),
    resendVerification: vi.fn(),
}));

import { register, resendVerification } from '../services/auth';

const mockRegister = vi.mocked(register);
const mockResendVerification = vi.mocked(resendVerification);

describe('Register page', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    const renderRegister = () => {
        render(
            <MemoryRouter initialEntries={['/register']}>
                <Register />
            </MemoryRouter>
        );
    };

    it('renders register heading', () => {
        renderRegister();
        expect(screen.getByRole('heading', { name: /register/i })).toBeInTheDocument();
    });

    it('renders email, first name, last name, password, and confirm password inputs', () => {
        renderRegister();
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    });

    it('renders register button', () => {
        renderRegister();
        expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
    });

    it('renders link to login page', () => {
        renderRegister();
        expect(screen.getByText(/log in here/i)).toBeInTheDocument();
    });

    it('shows password strength indicator', async () => {
        renderRegister();
        const passwordInput = screen.getByLabelText(/^password$/i);

        await userEvent.type(passwordInput, 'short');
        expect(screen.getByText(/too short/i)).toBeInTheDocument();

        await userEvent.clear(passwordInput);
        await userEvent.type(passwordInput, 'longpassword');
        expect(screen.getByText(/weak/i)).toBeInTheDocument();

        await userEvent.clear(passwordInput);
        await userEvent.type(passwordInput, 'Longpass1');
        expect(screen.getByText(/good/i)).toBeInTheDocument();

        await userEvent.clear(passwordInput);
        await userEvent.type(passwordInput, 'VeryLongPass12');
        expect(screen.getByText(/strong/i)).toBeInTheDocument();
    });

    it('shows password mismatch indicator', async () => {
        renderRegister();

        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Different1');

        expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    });

    it('shows error when passwords do not match on submit', async () => {
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Password2');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getAllByText(/passwords do not match/i).length).toBeGreaterThanOrEqual(1);
        });
        expect(mockRegister).not.toHaveBeenCalled();
    });

    it('shows error when password lacks uppercase on submit', async () => {
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'password1');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/at least one uppercase/i)).toBeInTheDocument();
        });
        expect(mockRegister).not.toHaveBeenCalled();
    });

    it('shows error when password lacks number on submit', async () => {
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Passwordd');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Passwordd');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/at least one number/i)).toBeInTheDocument();
        });
        expect(mockRegister).not.toHaveBeenCalled();
    });

    it('calls register on successful submission and shows email verification screen', async () => {
        mockRegister.mockResolvedValue(undefined);
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(mockRegister).toHaveBeenCalledWith('user@example.com', 'Password1', 'John', 'Doe');
        });

        await waitFor(() => {
            expect(screen.getByText(/check your email/i)).toBeInTheDocument();
            expect(screen.getByText(/user@example.com/i)).toBeInTheDocument();
        });
    });

    it('does not auto-login after registration', async () => {
        mockRegister.mockResolvedValue(undefined);
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/check your email/i)).toBeInTheDocument();
        });

        expect(screen.getByText(/go to login/i)).toBeInTheDocument();
    });

    it('shows resend button on verification screen and handles resend', async () => {
        mockRegister.mockResolvedValue(undefined);
        mockResendVerification.mockResolvedValue(undefined);
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/resend verification email/i)).toBeInTheDocument();
        });

        await userEvent.click(screen.getByText(/resend verification email/i));

        await waitFor(() => {
            expect(mockResendVerification).toHaveBeenCalledWith('user@example.com');
            expect(screen.getByText(/verification email resent/i)).toBeInTheDocument();
        });
    });

    it('displays error on failed registration', async () => {
        mockRegister.mockRejectedValue(new Error('Email already in use'));
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'existing@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/email already in use/i)).toBeInTheDocument();
        });
    });

    it('does not display error initially', () => {
        renderRegister();
        expect(screen.queryByText(/registration failed/i)).not.toBeInTheDocument();
    });

    it('disables button while submitting', async () => {
        mockRegister.mockImplementation(() => new Promise(() => {}));
        renderRegister();

        await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
        await userEvent.type(screen.getByLabelText(/first name/i), 'John');
        await userEvent.type(screen.getByLabelText(/last name/i), 'Doe');
        await userEvent.type(screen.getByLabelText(/^password$/i), 'Password1');
        await userEvent.type(screen.getByLabelText(/confirm password/i), 'Password1');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /creating account/i })).toBeDisabled();
        });
    });
});
