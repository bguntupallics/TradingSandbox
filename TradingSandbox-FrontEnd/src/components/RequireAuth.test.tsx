import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RequireAuth from './RequireAuth';

const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
    useAuth: () => mockUseAuth(),
}));

describe('RequireAuth', () => {
    it('shows loading state when auth is loading', () => {
        mockUseAuth.mockReturnValue({ user: null, loading: true });

        render(
            <MemoryRouter initialEntries={['/protected']}>
                <Routes>
                    <Route
                        path="/protected"
                        element={
                            <RequireAuth>
                                <div>Protected Content</div>
                            </RequireAuth>
                        }
                    />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Loading...')).toBeInTheDocument();
        expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    });

    it('renders children when user is authenticated', () => {
        mockUseAuth.mockReturnValue({
            user: { id: 1, username: 'test', email: 'test@example.com' },
            loading: false,
        });

        render(
            <MemoryRouter initialEntries={['/protected']}>
                <Routes>
                    <Route
                        path="/protected"
                        element={
                            <RequireAuth>
                                <div>Protected Content</div>
                            </RequireAuth>
                        }
                    />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.getByText('Protected Content')).toBeInTheDocument();
    });

    it('redirects to /login when user is not authenticated', () => {
        mockUseAuth.mockReturnValue({ user: null, loading: false });

        render(
            <MemoryRouter initialEntries={['/protected']}>
                <Routes>
                    <Route
                        path="/protected"
                        element={
                            <RequireAuth>
                                <div>Protected Content</div>
                            </RequireAuth>
                        }
                    />
                    <Route path="/login" element={<div>Login Page</div>} />
                </Routes>
            </MemoryRouter>
        );

        expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
        expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
});
