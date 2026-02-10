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
            className="theme-toggle-btn"
        >
            {theme === 'dark' ? '🌙 Dark' : '☀️ Light'}
        </button>
    );
}
