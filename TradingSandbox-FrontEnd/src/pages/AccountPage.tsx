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
                // update formValues to reflect any server-side changes
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
        <>
            <div className="container">
                <h1 className="text-light">Account</h1>

                {loading && <p>Loading…</p>}
                {error && <p className="error">{error}</p>}

                {account && (
                    <div className="panel account-panel space-y-6 mt-4">
                        {!isEditing && (
                            <button
                                className="edit-btn"
                                onClick={() => setIsEditing(true)}
                            >
                                <Pencil className="nav-icon text-light" />
                            </button>
                        )}

                        {!isEditing ? (
                            <>
                                <div className="flex items-center justify-center space-x-4">
                                    <span className="text-secondary">Username:</span>
                                    <span>{account.username}</span>
                                </div>

                                <div className="flex items-center justify-center space-x-4">
                                    <span className="text-secondary">Email:</span>
                                    <span>{account.email}</span>
                                </div>

                                <div className="flex items-center justify-center space-x-4">
                                    <span className="text-secondary">Name:</span>
                                    <span>
                                        {account.firstName} {account.lastName}
                                    </span>
                                </div>
                            </>
                        ) : (
                            <form onSubmit={handleSubmit} className="space-y-6">
                                <div className="flex items-center justify-center space-x-4">
                                    <label className="text-secondary">Username:</label>
                                    <input
                                        type="text"
                                        name="username"
                                        value={formValues.username}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>

                                <div className="flex items-center justify-center space-x-4">
                                    <label className="text-secondary">Email:</label>
                                    <input
                                        type="email"
                                        name="email"
                                        value={formValues.email}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>

                                <div className="flex items-center justify-center space-x-4">
                                    <label className="text-secondary">First Name:</label>
                                    <input
                                        type="text"
                                        name="firstName"
                                        value={formValues.firstName}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>

                                <div className="flex items-center justify-center space-x-4">
                                    <label className="text-secondary">Last Name:</label>
                                    <input
                                        type="text"
                                        name="lastName"
                                        value={formValues.lastName}
                                        onChange={handleChange}
                                        required
                                    />
                                </div>

                                <div className="flex justify-center space-x-4">
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
                                    <button type="submit" className="btn">
                                        Save Changes
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                )}
            </div>

            <div className="container flex justify-center mt-4">
                <button
                    className="btn"
                    onClick={async () => {
                        await logout();
                        window.location.href = '/';
                    }}
                >
                    Log Out
                </button>
            </div>
        </>
    );
}
