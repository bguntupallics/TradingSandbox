import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

// Mock all child components to isolate routing logic
vi.mock('./components/HomeRedirect', () => ({
    default: () => <div>HomeRedirect</div>,
}));

vi.mock('./pages/Login', () => ({
    default: () => <div>Login Page</div>,
}));

vi.mock('./pages/Register', () => ({
    default: () => <div>Register Page</div>,
}));

vi.mock('./pages/Dashboard', () => ({
    default: () => <div>Dashboard Page</div>,
}));

vi.mock('./pages/SearchPage.tsx', () => ({
    default: () => <div>Search Page</div>,
}));

vi.mock('./pages/AccountPage.tsx', () => ({
    default: () => <div>Account Page</div>,
}));

vi.mock('./components/RequireAuth', () => ({
    default: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('./components/LoggedInLayout.tsx', () => ({
    LoggedInLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

describe('App routing', () => {
    it('renders HomeRedirect at /', () => {
        render(
            <MemoryRouter initialEntries={['/']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('HomeRedirect')).toBeInTheDocument();
    });

    it('renders Login page at /login', () => {
        render(
            <MemoryRouter initialEntries={['/login']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('Login Page')).toBeInTheDocument();
    });

    it('renders Register page at /register', () => {
        render(
            <MemoryRouter initialEntries={['/register']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('Register Page')).toBeInTheDocument();
    });

    it('renders Dashboard at /dashboard', () => {
        render(
            <MemoryRouter initialEntries={['/dashboard']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('Dashboard Page')).toBeInTheDocument();
    });

    it('renders SearchPage at /search', () => {
        render(
            <MemoryRouter initialEntries={['/search']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('Search Page')).toBeInTheDocument();
    });

    it('renders AccountPage at /account', () => {
        render(
            <MemoryRouter initialEntries={['/account']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('Account Page')).toBeInTheDocument();
    });

    it('renders 404 for unknown routes', () => {
        render(
            <MemoryRouter initialEntries={['/nonexistent']}>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText(/404/)).toBeInTheDocument();
    });
});
