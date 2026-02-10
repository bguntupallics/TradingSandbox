import { useState } from 'react';
import { useNavigate, useLocation, Link, useSearchParams } from 'react-router-dom';
import { login, resendVerification } from '../services/auth';
import { useAuth } from '../contexts/AuthContext';

export default function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showResend, setShowResend] = useState(false);
    const [resendSuccess, setResendSuccess] = useState(false);

    const navigate = useNavigate();
    const location = useLocation() as { state?: { from?: string } };
    const from = location.state?.from || '/dashboard';
    const { user, refreshAuth } = useAuth();
    const [searchParams] = useSearchParams();

    const verified = searchParams.get('verified') === 'true';

    // If already logged in, redirect
    if (user) {
        navigate('/dashboard', { replace: true });
        return null;
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setShowResend(false);
        setResendSuccess(false);
        setIsSubmitting(true);
        try {
            await login(email, password);
            await refreshAuth();
            navigate(from, { replace: true });
        } catch (err) {
            const msg = (err as Error).message;
            if (msg === 'Email not verified') {
                setError('Your email is not verified yet. Please check your inbox.');
                setShowResend(true);
            } else {
                setError(msg || 'Invalid email or password');
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleResend = async () => {
        try {
            await resendVerification(email);
            setResendSuccess(true);
        } catch (err) {
            setError((err as Error).message);
        }
    };

    return (
        <div style={{ maxWidth: 400, margin: '2rem auto' }}>
            <h1>Login</h1>

            {verified && (
                <div style={{ color: 'green', marginBottom: '1rem', padding: '0.5rem', border: '1px solid green', borderRadius: 4 }}>
                    Email verified successfully! You can now log in.
                </div>
            )}

            {error && <div style={{ color: 'red', marginBottom: '0.5rem' }}>{error}</div>}

            {showResend && !resendSuccess && (
                <button
                    type="button"
                    onClick={handleResend}
                    style={{ marginBottom: '1rem', cursor: 'pointer', textDecoration: 'underline', background: 'none', border: 'none', color: '#0066cc' }}
                >
                    Resend verification email
                </button>
            )}
            {resendSuccess && (
                <div style={{ color: 'green', marginBottom: '1rem' }}>
                    Verification email sent! Check your inbox.
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div>
                    <label>
                        Email<br/>
                        <input
                            type="email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
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
                <button type="submit" disabled={isSubmitting} style={{ marginTop: '1rem' }}>
                    {isSubmitting ? 'Logging in...' : 'Log In'}
                </button>
            </form>
            <p style={{ marginTop: '1rem' }}>
                Don't have an account? <Link to="/register">Register here</Link>.
            </p>
        </div>
    );
}
