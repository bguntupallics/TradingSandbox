import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Navbar } from './Navbar';

// Mock ThemeToggle to avoid side-effects
vi.mock('./ThemeToggle.tsx', () => ({
    ThemeToggle: () => <button data-testid="theme-toggle">Toggle Theme</button>,
}));

describe('Navbar', () => {
    const renderNavbar = () => {
        render(
            <MemoryRouter>
                <Navbar />
            </MemoryRouter>
        );
    };

    it('renders the navigation element', () => {
        renderNavbar();
        expect(screen.getByRole('navigation')).toBeInTheDocument();
    });

    it('renders Home link', () => {
        renderNavbar();
        expect(screen.getByText('Home')).toBeInTheDocument();
    });

    it('renders Search link', () => {
        renderNavbar();
        expect(screen.getByText('Search')).toBeInTheDocument();
    });

    it('renders Account link', () => {
        renderNavbar();
        expect(screen.getByText('Account')).toBeInTheDocument();
    });

    it('renders the theme toggle', () => {
        renderNavbar();
        expect(screen.getByTestId('theme-toggle')).toBeInTheDocument();
    });

    it('renders the logo image', () => {
        renderNavbar();
        const logo = screen.getByAltText('TradingSandbox');
        expect(logo).toBeInTheDocument();
        expect(logo).toHaveAttribute('src', '/logo.svg');
    });

    it('has correct link destinations', () => {
        renderNavbar();
        const links = screen.getAllByRole('link');
        const hrefs = links.map((l) => l.getAttribute('href'));

        expect(hrefs).toContain('/');
        expect(hrefs).toContain('/search');
        expect(hrefs).toContain('/account');
    });
});
