import { useState, useEffect } from 'react';
import { fetchWithJwt } from '../services/api';
import '../styles/global.css';

export default function Dashboard() {
    const [balance, setBalance] = useState<number | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setLoading(true);
        fetchWithJwt<number>('/api/account/balance')
            .then(amount => {
                setBalance(amount);
                setError(null);
            })
            .catch(err => {
                setError(err.message);
                setBalance(null);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    return (
        <div className="container">
            <h1>Dashboard</h1>

            {loading && <p>Loading…</p>}
            {error && <p className="error">{error}</p>}

            {balance !== null && (
                <div className="panel mt-4">
                    <h2>Buying Power</h2>
                    <p className="text-xl">
                        $ {balance.toLocaleString()}
                    </p>
                </div>
            )}
        </div>
    );
}
