import { useState, useEffect, type ChangeEvent, type FormEvent } from 'react';
import { fetchApi } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Pencil } from 'lucide-react';
import '../styles/global.css';

interface Account {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    cashBalance: number;
}

export default function AccountPage() {
    const { logout } = useAuth();
    const [account, setAccount] = useState<Account | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const [isEditing, setIsEditing] = useState<boolean>(false);
    const [formValues, setFormValues] = useState({
        username: '',
        email: '',
        firstName: '',
        lastName: ''
    });

    useEffect(() => {
        setLoading(true);
        fetchApi<Account>('/api/account')
            .then(data => {
                setAccount(data);
                setError(null);
                setFormValues({
                    username: data.username,
                    email: data.email,
                    firstName: data.firstName,
                    lastName: data.lastName
                });
            })
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, []);

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormValues(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);
        fetchApi<Account>('/api/account/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formValues)
        })
            .then(updated => {
                setAccount(updated);
                setIsEditing(false);
                setError(null);
                setFormValues({
                    username: updated.username,
                    email: updated.email,
                    firstName: updated.firstName,
                    lastName: updated.lastName
                });
            })
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    };

    return (
        <div className="container account-section fade-in">
            <div className="account-header">
                <h1>Account</h1>
                {account && !loading && !isEditing && (
                    <button
                        className="edit-btn"
                        onClick={() => setIsEditing(true)}
                    >
                        <Pencil className="nav-icon" />
                    </button>
                )}
            </div>

            {loading && <p className="loading-spinner">Loading...</p>}
            {error && <p className="error">{error}</p>}

            {account && !loading && (
                <div className="panel account-panel">
                    {!isEditing ? (
                        <>
                            <div className="account-row">
                                <span className="account-label">Username</span>
                                <span className="account-value">{account.username}</span>
                            </div>
                            <div className="account-row">
                                <span className="account-label">Email</span>
                                <span className="account-value">{account.email}</span>
                            </div>
                            <div className="account-row">
                                <span className="account-label">Name</span>
                                <span className="account-value">
                                    {account.firstName} {account.lastName}
                                </span>
                            </div>
                        </>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label className="account-label">Username</label>
                                <input
                                    type="text"
                                    name="username"
                                    value={formValues.username}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="account-label">Email</label>
                                <input
                                    type="email"
                                    name="email"
                                    value={formValues.email}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="account-label">First Name</label>
                                <input
                                    type="text"
                                    name="firstName"
                                    value={formValues.firstName}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="account-label">Last Name</label>
                                <input
                                    type="text"
                                    name="lastName"
                                    value={formValues.lastName}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="flex gap-2 justify-center" style={{ marginTop: '0.5rem' }}>
                                <button
                                    type="button"
                                    className="btn"
                                    onClick={() => {
                                        if (account) {
                                            setFormValues({
                                                username: account.username,
                                                email: account.email,
                                                firstName: account.firstName,
                                                lastName: account.lastName
                                            });
                                        }
                                        setIsEditing(false);
                                    }}
                                >
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Save Changes
                                </button>
                            </div>
                        </form>
                    )}
                </div>
            )}

            {account && !loading && (
                <div className="account-actions">
                    <button
                        className="btn btn-danger"
                        onClick={async () => {
                            await logout();
                            window.location.href = '/';
                        }}
                    >
                        Log Out
                    </button>
                </div>
            )}
        </div>
    );
}
