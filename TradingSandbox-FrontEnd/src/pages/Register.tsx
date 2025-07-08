import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../services/auth';

export default function Register() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string|null>(null);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        try {
            // call your backend register endpoint
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
            });
            if (!res.ok) throw new Error('Registration failed');
            // auto-login on success
            await login(username, password);
            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError((err as Error).message);
        }
    };

    return (
        <div style={{ maxWidth: 400, margin: '2rem auto' }}>
            <h1>Register</h1>
            {error && <div style={{ color: 'red' }}>{error}</div>}
            <form onSubmit={handleSubmit}>
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
                <br/><br/>
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
                <br/><br/>
                <button type="submit">Register</button>
            </form>
            <p style={{ marginTop: '1rem' }}>
                Already have an account? <Link to="/login">Log in here</Link>.
            </p>
        </div>
    );
}
