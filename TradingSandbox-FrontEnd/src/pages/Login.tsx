import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { login, isLoggedIn } from '../services/auth';

export default function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);

    const navigate = useNavigate();
    const location = useLocation() as { state?: { from?: string } };
    const from = location.state?.from || '/dashboard';

    // If already logged in, kick straight to dashboard
    useEffect(() => {
        if (isLoggedIn()) {
            navigate('/dashboard', { replace: true });
        }
    }, [navigate]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        try {
            await login(username, password);
            navigate(from, { replace: true });
        } catch {
            setError('Invalid username or password');
        }
    };

    return (
        <div style={{ maxWidth: 400, margin: '2rem auto' }}>
            <h1>Login</h1>
            {error && <div style={{ color: 'red' }}>{error}</div>}
            <form onSubmit={handleSubmit}>
                <div>
                    <label>
                        Username<br/>
                        <input
                            type="text"
                            value={username}
                            onChange={e => setUsername(e.target.value)}
                            required
                            style={{ width: '100%' }}
                        />
                    </label>
                </div>
                <div style={{ marginTop: '1rem' }}>
                    <label>
                        Password<br/>
                        <input
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            required
                            style={{ width: '100%' }}
                        />
                    </label>
                </div>
                <button type="submit" style={{ marginTop: '1rem' }}>
                    Log In
                </button>
            </form>
            <p style={{ marginTop: '1rem' }}>
                Don’t have an account? <Link to="/register">Register here</Link>.
            </p>
        </div>
    );
}
