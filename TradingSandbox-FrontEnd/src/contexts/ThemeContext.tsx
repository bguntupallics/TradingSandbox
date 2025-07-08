import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'

// 1. Define the shape of your context
interface ThemeContextType {
    theme: 'light' | 'dark'
    toggleTheme: () => void
}

// 2. Create it with a default (won’t actually be used)
const ThemeContext = createContext<ThemeContextType>({
    theme: 'light',
    toggleTheme: () => {}
})

// 3. Provider component
export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<'light' | 'dark'>(() => {
        // 3a. On mount, read from localStorage or OS preference
        const saved = localStorage.getItem('theme') as 'light' | 'dark' | null
        if (saved) return saved
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    })

    // 3b. Side effect: apply class & persist
    useEffect(() => {
        const root = document.documentElement
        root.classList.toggle('dark', theme === 'dark')
        localStorage.setItem('theme', theme)
    }, [theme])

    const toggleTheme = () => {
        setTheme(curr => (curr === 'light' ? 'dark' : 'light'))
    }

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme }}>
            {children}
        </ThemeContext.Provider>
    )
}

// 4. Custom hook for easy consumption
export function useTheme() {
    return useContext(ThemeContext)
}
