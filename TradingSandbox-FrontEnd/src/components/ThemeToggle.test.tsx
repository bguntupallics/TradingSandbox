import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeToggle } from './ThemeToggle';

// Mock dependencies
vi.mock('../contexts/ThemeContext', () => ({
    useTheme: vi.fn(),
}));

vi.mock('../services/api', () => ({
    fetchWithJwt: vi.fn(),
}));

import { useTheme } from '../contexts/ThemeContext';
import { fetchWithJwt } from '../services/api';

const mockUseTheme = vi.mocked(useTheme);
const mockFetchWithJwt = vi.mocked(fetchWithJwt);

describe('ThemeToggle', () => {
    const mockToggleTheme = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders light theme button text when theme is light', () => {
        mockUseTheme.mockReturnValue({ theme: 'light', toggleTheme: mockToggleTheme });
        mockFetchWithJwt.mockResolvedValue('light');

        render(<ThemeToggle />);

        expect(screen.getByRole('button')).toHaveTextContent('Light');
    });

    it('renders dark theme button text when theme is dark', () => {
        mockUseTheme.mockReturnValue({ theme: 'dark', toggleTheme: mockToggleTheme });
        mockFetchWithJwt.mockResolvedValue('dark');

        render(<ThemeToggle />);

        expect(screen.getByRole('button')).toHaveTextContent('Dark');
    });

    it('calls API to change theme on click', async () => {
        mockUseTheme.mockReturnValue({ theme: 'light', toggleTheme: mockToggleTheme });
        mockFetchWithJwt.mockResolvedValue('light');

        render(<ThemeToggle />);

        await userEvent.click(screen.getByRole('button'));

        expect(mockFetchWithJwt).toHaveBeenCalledWith('/api/account/change-theme', {
            method: 'POST',
        });
    });

    it('calls toggleTheme after successful API call on click', async () => {
        mockUseTheme.mockReturnValue({ theme: 'light', toggleTheme: mockToggleTheme });
        mockFetchWithJwt.mockResolvedValue('light');

        render(<ThemeToggle />);

        await userEvent.click(screen.getByRole('button'));

        await waitFor(() => {
            expect(mockToggleTheme).toHaveBeenCalled();
        });
    });

    it('does not crash when API change-theme fails on click', async () => {
        mockUseTheme.mockReturnValue({ theme: 'light', toggleTheme: mockToggleTheme });
        // First call is for the useEffect theme sync, second is for click
        mockFetchWithJwt
            .mockResolvedValueOnce('light') // useEffect
            .mockRejectedValueOnce(new Error('Network error')); // click

        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        render(<ThemeToggle />);

        await userEvent.click(screen.getByRole('button'));

        // Should not crash
        consoleSpy.mockRestore();
    });
});
