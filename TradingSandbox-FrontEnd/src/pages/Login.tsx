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
        <div className="auth-page">
            <div className="auth-card">
                <h1>Login</h1>

                {verified && (
                    <div className="auth-alert success">
                        Email verified successfully! You can now log in.
                    </div>
                )}

                {error && <div className="auth-alert error">{error}</div>}

                {showResend && !resendSuccess && (
                    <button
                        type="button"
                        onClick={handleResend}
                        className="resend-link"
                    >
                        Resend verification email
                    </button>
                )}
                {resendSuccess && (
                    <div className="auth-alert success">
                        Verification email sent! Check your inbox.
                    </div>
                )}

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <label htmlFor="login-email">Email</label>
                        <input
                            id="login-email"
                            type="email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="login-password">Password</label>
                        <input
                            id="login-password"
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? 'Logging in...' : 'Log In'}
                    </button>
                </form>

                <p className="auth-footer">
                    Don't have an account? <Link to="/register">Register here</Link>
                </p>
            </div>
        </div>
    );
}
