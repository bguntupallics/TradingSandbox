import { type ReactNode } from 'react'
import { Navbar } from './Navbar'
import { Footer } from './Footer'

interface DashboardLayoutProps {
    children: ReactNode
}

export function LoggedInLayout({ children }: DashboardLayoutProps) {
    return (
        <div className="app-layout">
            <Navbar />
            <main className="app-main">
                {children}
            </main>
            <Footer />
        </div>
    )
}
