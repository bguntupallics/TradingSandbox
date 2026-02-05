import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Register from './Register';

vi.mock('../services/auth', () => ({
    login: vi.fn(),
}));

import { login } from '../services/auth';

const mockLogin = vi.mocked(login);

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

    it('renders username and password inputs', () => {
        renderRegister();
        expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    });

    it('renders register button', () => {
        renderRegister();
        expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
    });

    it('renders link to login page', () => {
        renderRegister();
        expect(screen.getByText(/log in here/i)).toBeInTheDocument();
    });

    it('calls fetch and login on successful registration', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({}),
        });
        mockLogin.mockResolvedValue(undefined);

        renderRegister();

        await userEvent.type(screen.getByLabelText(/username/i), 'newuser');
        await userEvent.type(screen.getByLabelText(/password/i), 'password123');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
            }));
        });

        expect(mockLogin).toHaveBeenCalledWith('newuser', 'password123');
    });

    it('displays error on failed registration', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: false,
        });

        renderRegister();

        await userEvent.type(screen.getByLabelText(/username/i), 'newuser');
        await userEvent.type(screen.getByLabelText(/password/i), 'pass');
        await userEvent.click(screen.getByRole('button', { name: /register/i }));

        await waitFor(() => {
            expect(screen.getByText(/registration failed/i)).toBeInTheDocument();
        });
    });

    it('does not display error initially', () => {
        renderRegister();
        expect(screen.queryByText(/registration failed/i)).not.toBeInTheDocument();
    });
});
