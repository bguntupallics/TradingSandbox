import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, useTheme } from './ThemeContext';

// Test component that uses the theme context
function ThemeConsumer() {
    const { theme, toggleTheme } = useTheme();
    return (
        <div>
            <span data-testid="theme">{theme}</span>
            <button onClick={toggleTheme}>Toggle</button>
        </div>
    );
}

describe('ThemeContext', () => {
    beforeEach(() => {
        localStorage.clear();
        document.documentElement.classList.remove('dark');
    });

    it('defaults to light theme when no localStorage value', () => {
        // Mock matchMedia to return false for dark preference
        window.matchMedia = vi.fn().mockImplementation((query) => ({
            matches: false,
            media: query,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme').textContent).toBe('light');
    });

    it('reads theme from localStorage on mount', () => {
        localStorage.setItem('theme', 'dark');

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme').textContent).toBe('dark');
    });

    it('uses OS dark mode preference when no localStorage value', () => {
        window.matchMedia = vi.fn().mockImplementation((query) => ({
            matches: query === '(prefers-color-scheme: dark)',
            media: query,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme').textContent).toBe('dark');
    });

    it('toggles theme from light to dark', async () => {
        window.matchMedia = vi.fn().mockImplementation((query) => ({
            matches: false,
            media: query,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme').textContent).toBe('light');

        await userEvent.click(screen.getByText('Toggle'));

        expect(screen.getByTestId('theme').textContent).toBe('dark');
    });

    it('toggles theme from dark to light', async () => {
        localStorage.setItem('theme', 'dark');

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        expect(screen.getByTestId('theme').textContent).toBe('dark');

        await userEvent.click(screen.getByText('Toggle'));

        expect(screen.getByTestId('theme').textContent).toBe('light');
    });

    it('persists theme to localStorage on toggle', async () => {
        window.matchMedia = vi.fn().mockImplementation((query) => ({
            matches: false,
            media: query,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        await userEvent.click(screen.getByText('Toggle'));

        expect(localStorage.getItem('theme')).toBe('dark');
    });

    it('applies dark class to document.documentElement', async () => {
        window.matchMedia = vi.fn().mockImplementation((query) => ({
            matches: false,
            media: query,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));

        render(
            <ThemeProvider>
                <ThemeConsumer />
            </ThemeProvider>
        );

        await userEvent.click(screen.getByText('Toggle'));

        expect(document.documentElement.classList.contains('dark')).toBe(true);
    });
});
