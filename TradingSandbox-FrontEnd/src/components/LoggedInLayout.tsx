import { type ReactNode } from 'react'
import { Navbar } from './Navbar'

interface DashboardLayoutProps {
    children: ReactNode
}

export function LoggedInLayout({ children }: DashboardLayoutProps) {
    return (
        <div className="min-h-screen bg-neutral-50 dark:bg-neutral-900 text-neutral-800 dark:text-neutral-100">
            <Navbar />
            <main className="p-6">
                {children}
            </main>
        </div>
    )
}
