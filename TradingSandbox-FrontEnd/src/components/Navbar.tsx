import { NavLink } from 'react-router-dom'
import {
    Home,
    User,
    Search
} from 'lucide-react';
import { ThemeToggle } from './ThemeToggle.tsx';

export function Navbar() {
    return (
        <nav className="navbar">
            {/* Left: Logo (optional) */}
            <div className="navbar-logo">
                <NavLink to="/">
                    <img
                        src="/logo.svg"
                        alt="TradingSandbox"
                        className="logo-img"
                    />
                </NavLink>
            </div>

            {/* Center: nav items */}
            <ul className="navbar-nav">
                <li>
                    <NavLink to="/" end className="nav-item">
                        <Home className="nav-icon"/>
                        <span className="nav-label">Home</span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/search" className="nav-item">
                        <Search className="nav-icon"/>
                        <span className="nav-label">Search</span>
                    </NavLink>
                </li>

                <li>
                    <NavLink to="/account" className="nav-item">
                        <User className="nav-icon"/>
                        <span className="nav-label">Account</span>
                    </NavLink>
                </li>
            </ul>

            {/* Right: Theme toggle */}
            <div className="navbar-controls">
                <ThemeToggle/>
            </div>
        </nav>
    )
}
