import { type ReactNode } from 'react'
import { Navbar } from './Navbar'
import { Footer } from './Footer'

interface DashboardLayoutProps {
    children: ReactNode
}

export function LoggedInLayout({ children }: DashboardLayoutProps) {
    return (
        <div className="min-h-screen bg-neutral-50 dark:bg-neutral-900 text-neutral-800 dark:text-neutral-100" style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
            <Navbar />
            <main className="p-6" style={{ flexGrow: 1 }}>
                {children}
            </main>
            <Footer />
        </div>
    )
}
