import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { register, resendVerification } from '../services/auth';

function getPasswordStrength(pw: string): { label: string; color: string } {
    if (pw.length < 8) return { label: 'Too short', color: '#cc0000' };
    const hasUpper = /[A-Z]/.test(pw);
    const hasNumber = /[0-9]/.test(pw);
    if (!hasUpper || !hasNumber) return { label: 'Weak', color: '#cc6600' };
    if (pw.length >= 12) return { label: 'Strong', color: '#009900' };
    return { label: 'Good', color: '#339900' };
}

export default function Register() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [registered, setRegistered] = useState(false);
    const [resendSuccess, setResendSuccess] = useState(false);

    const strength = getPasswordStrength(password);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }
        if (password.length < 8) {
            setError('Password must be at least 8 characters');
            return;
        }
        if (!/[A-Z]/.test(password)) {
            setError('Password must contain at least one uppercase letter');
            return;
        }
        if (!/[0-9]/.test(password)) {
            setError('Password must contain at least one number');
            return;
        }

        setIsSubmitting(true);
        try {
            await register(email, password, firstName, lastName);
            setRegistered(true);
        } catch (err) {
            setError((err as Error).message);
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

    if (registered) {
        return (
            <div className="auth-page">
                <div className="auth-card" style={{ textAlign: 'center' }}>
                    <h1>Check Your Email</h1>
                    <p style={{ marginTop: '0.5rem' }}>
                        We've sent a verification link to <strong>{email}</strong>.
                    </p>
                    <p className="text-secondary" style={{ marginTop: '0.5rem', fontSize: '0.9rem' }}>
                        Click the link in the email to verify your account, then come back to log in.
                    </p>
                    {!resendSuccess ? (
                        <button
                            type="button"
                            onClick={handleResend}
                            className="resend-link"
                            style={{ marginTop: '1rem' }}
                        >
                            Resend verification email
                        </button>
                    ) : (
                        <div className="auth-alert success" style={{ marginTop: '1rem' }}>
                            Verification email resent!
                        </div>
                    )}
                    <p className="auth-footer">
                        <Link to="/login">Go to Login</Link>
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>Register</h1>

                {error && <div className="auth-alert error">{error}</div>}

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <label htmlFor="reg-email">Email</label>
                        <input
                            id="reg-email"
                            type="email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="reg-first-name">First Name</label>
                        <input
                            id="reg-first-name"
                            type="text"
                            value={firstName}
                            onChange={e => setFirstName(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="reg-last-name">Last Name</label>
                        <input
                            id="reg-last-name"
                            type="text"
                            value={lastName}
                            onChange={e => setLastName(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="reg-password">Password</label>
                        <input
                            id="reg-password"
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            required
                            minLength={8}
                        />
                        {password.length > 0 && (
                            <div className="password-strength" style={{ color: strength.color }}>
                                {strength.label}
                                <span className="password-hint">
                                    (min 8 chars, 1 uppercase, 1 number)
                                </span>
                            </div>
                        )}
                    </div>
                    <div className="form-group">
                        <label htmlFor="reg-confirm-password">Confirm Password</label>
                        <input
                            id="reg-confirm-password"
                            type="password"
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                            required
                        />
                        {confirmPassword.length > 0 && confirmPassword !== password && (
                            <div className="password-strength" style={{ color: '#cc0000' }}>
                                Passwords do not match
                            </div>
                        )}
                    </div>
                    <button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? 'Creating account...' : 'Register'}
                    </button>
                </form>

                <p className="auth-footer">
                    Already have an account? <Link to="/login">Log in here</Link>
                </p>
            </div>
        </div>
    );
}
