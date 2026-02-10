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
            <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center' }}>
                <h1>Check Your Email</h1>
                <p style={{ marginTop: '1rem' }}>
                    We've sent a verification link to <strong>{email}</strong>.
                </p>
                <p style={{ marginTop: '0.5rem', color: '#666' }}>
                    Click the link in the email to verify your account, then come back to log in.
                </p>
                {!resendSuccess ? (
                    <button
                        type="button"
                        onClick={handleResend}
                        style={{ marginTop: '1rem', cursor: 'pointer', textDecoration: 'underline', background: 'none', border: 'none', color: '#0066cc' }}
                    >
                        Resend verification email
                    </button>
                ) : (
                    <p style={{ color: 'green', marginTop: '1rem' }}>
                        Verification email resent!
                    </p>
                )}
                <p style={{ marginTop: '1.5rem' }}>
                    <Link to="/login">Go to Login</Link>
                </p>
            </div>
        );
    }

    return (
        <div style={{ maxWidth: 400, margin: '2rem auto' }}>
            <h1>Register</h1>
            {error && <div style={{ color: 'red', marginBottom: '0.5rem' }}>{error}</div>}
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
                        First Name<br/>
                        <input
                            type="text"
                            value={firstName}
                            onChange={e => setFirstName(e.target.value)}
                            required
                            style={{ width: '100%' }}
                        />
                    </label>
                </div>
                <div style={{ marginTop: '1rem' }}>
                    <label>
                        Last Name<br/>
                        <input
                            type="text"
                            value={lastName}
                            onChange={e => setLastName(e.target.value)}
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
                            minLength={8}
                            style={{ width: '100%' }}
                        />
                    </label>
                    {password.length > 0 && (
                        <div style={{ fontSize: '0.85rem', color: strength.color, marginTop: '0.25rem' }}>
                            {strength.label}
                            <span style={{ color: '#888', marginLeft: '0.5rem' }}>
                                (min 8 chars, 1 uppercase, 1 number)
                            </span>
                        </div>
                    )}
                </div>
                <div style={{ marginTop: '1rem' }}>
                    <label>
                        Confirm Password<br/>
                        <input
                            type="password"
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                            required
                            style={{ width: '100%' }}
                        />
                    </label>
                    {confirmPassword.length > 0 && confirmPassword !== password && (
                        <div style={{ fontSize: '0.85rem', color: '#cc0000', marginTop: '0.25rem' }}>
                            Passwords do not match
                        </div>
                    )}
                </div>
                <button type="submit" disabled={isSubmitting} style={{ marginTop: '1rem' }}>
                    {isSubmitting ? 'Creating account...' : 'Register'}
                </button>
            </form>
            <p style={{ marginTop: '1rem' }}>
                Already have an account? <Link to="/login">Log in here</Link>.
            </p>
        </div>
    );
}
