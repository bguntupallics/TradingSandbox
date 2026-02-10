import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import HomeRedirect from './HomeRedirect';

const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
    useAuth: () => mockUseAuth(),
}));

describe('HomeRedirect', () => {
    it('shows loading state when auth is loading', () => {
        mockUseAuth.mockReturnValue({ user: null, loading: true });

        render(
            <MemoryRouter initialEntries={['/']}>
                <Routes>
                    <Route path="/" element={<HomeRedirect />} />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('redirects to /dashboard when user is authenticated', () => {
        mockUseAuth.mockReturnValue({
            user: { id: 1, username: 'test', email: 'test@example.com' },
            loading: false,
        });

        render(
            <MemoryRouter initialEntries={['/']}>
                <Routes>
                    <Route path="/" element={<HomeRedirect />} />
                    <Route path="/dashboard" element={<div>Dashboard Page</div>} />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Dashboard Page')).toBeInTheDocument();
    });

    it('redirects to /login when user is not authenticated', () => {
        mockUseAuth.mockReturnValue({ user: null, loading: false });

        render(
            <MemoryRouter initialEntries={['/']}>
                <Routes>
                    <Route path="/" element={<HomeRedirect />} />
                    <Route path="/login" element={<div>Login Page</div>} />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
});
