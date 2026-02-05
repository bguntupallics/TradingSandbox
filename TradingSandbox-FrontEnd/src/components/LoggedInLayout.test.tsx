import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LoggedInLayout } from './LoggedInLayout';

// Mock Navbar to isolate the layout test
vi.mock('./Navbar', () => ({
    Navbar: () => <nav data-testid="navbar">Mocked Navbar</nav>,
}));

describe('LoggedInLayout', () => {
    it('renders children', () => {
        render(
            <MemoryRouter>
                <LoggedInLayout>
                    <div>Child Content</div>
                </LoggedInLayout>
            </MemoryRouter>
        );

        expect(screen.getByText('Child Content')).toBeInTheDocument();
    });

    it('renders the Navbar', () => {
        render(
            <MemoryRouter>
                <LoggedInLayout>
                    <div>Content</div>
                </LoggedInLayout>
            </MemoryRouter>
        );

        expect(screen.getByTestId('navbar')).toBeInTheDocument();
    });

    it('wraps content in a main element', () => {
        render(
            <MemoryRouter>
                <LoggedInLayout>
                    <div data-testid="inner">Inner</div>
                </LoggedInLayout>
            </MemoryRouter>
        );

        const main = screen.getByRole('main');
        expect(main).toBeInTheDocument();
        expect(main).toContainElement(screen.getByTestId('inner'));
    });
});
