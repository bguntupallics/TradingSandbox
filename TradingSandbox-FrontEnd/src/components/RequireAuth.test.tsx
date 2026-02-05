import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RequireAuth from './RequireAuth';

vi.mock('../services/auth', () => ({
    isLoggedIn: vi.fn(),
}));

import { isLoggedIn } from '../services/auth';

const mockIsLoggedIn = vi.mocked(isLoggedIn);

describe('RequireAuth', () => {
    it('renders children when logged in', () => {
        mockIsLoggedIn.mockReturnValue(true);

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

    it('redirects to /login when not logged in', () => {
        mockIsLoggedIn.mockReturnValue(false);

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
