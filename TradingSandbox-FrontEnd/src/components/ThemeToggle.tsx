import { useEffect } from 'react';
import { useTheme } from '../contexts/ThemeContext';
import { fetchWithJwt } from '../services/api';

export function ThemeToggle() {
    const { theme, toggleTheme } = useTheme();

    // Sync initial theme from server
    useEffect(() => {
        fetchWithJwt<string>('/api/account/theme')
            .then(serverTheme => {
                const normalized = serverTheme.toLowerCase();
                if ((normalized === 'dark' && theme !== 'dark') ||
                    (normalized === 'light' && theme !== 'light')) {
                    toggleTheme();
                }
            })
            .catch(err => console.error('Failed to load theme:', err));
    }, [theme, toggleTheme]);

    const handleClick = async () => {
        try {
            // flip on server
            await fetchWithJwt('/api/account/change-theme', {
                method: 'POST'
            });
            // then flip locally
            toggleTheme();
        } catch (err) {
            console.error('Failed to change theme:', err);
        }
    };

    return (
        <button
            onClick={handleClick}
            className="p-2 rounded-lg bg-neutral-200 dark:bg-neutral-700 focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
        >
            {theme === 'dark' ? '🌙 Dark' : '☀️ Light'}
        </button>
    );
}
