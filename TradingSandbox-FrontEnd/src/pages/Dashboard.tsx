import { useState, useEffect } from 'react';
import { fetchPortfolio, fetchTradeHistory } from '../services/api';
import type { PortfolioData, TradeHistoryItem } from '../services/api';
import { useNavigate } from 'react-router-dom';
import '../styles/global.css';

export default function Dashboard() {
    const [portfolio, setPortfolio] = useState<PortfolioData | null>(null);
    const [trades, setTrades] = useState<TradeHistoryItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        Promise.all([
            fetchPortfolio(),
            fetchTradeHistory(),
        ])
            .then(([portfolioData, tradeData]) => {
                setPortfolio(portfolioData);
                setTrades(tradeData);
                setError(null);
            })
            .catch(err => {
                setError(err.message);
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="container">
                <h1>Dashboard</h1>
                <p>Loading...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="container">
                <h1>Dashboard</h1>
                <p className="error">{error}</p>
            </div>
        );
    }

    if (!portfolio) return null;

    const totalGainLoss = portfolio.totalGainLoss;
    const totalCostBasis = portfolio.holdingsValue - totalGainLoss;
    const totalGainLossPercent = totalCostBasis !== 0
        ? (totalGainLoss / totalCostBasis) * 100
        : 0;
    const isPositive = totalGainLoss >= 0;

    const formatCurrency = (val: number) =>
        '$' + Math.abs(val).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    const formatDate = (dateStr: string) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    };

    return (
        <div className="container dashboard">
            <h1>Dashboard</h1>

            {/* Portfolio Value Header */}
            <div className="panel portfolio-header">
                <span className="text-secondary">Total Portfolio Value</span>
                <h2 className="portfolio-value">
                    {formatCurrency(portfolio.totalPortfolioValue)}
                </h2>
                {portfolio.holdings.length > 0 && (
                    <span className={`portfolio-gain ${isPositive ? 'positive' : 'negative'}`}>
                        {isPositive ? '+' : '-'}{formatCurrency(totalGainLoss)} ({isPositive ? '+' : '-'}{Math.abs(totalGainLossPercent).toFixed(2)}%) All Time
                    </span>
                )}
            </div>

            {/* Buying Power */}
            <div className="panel mt-1">
                <div className="buying-power-row">
                    <span className="text-secondary">Buying Power</span>
                    <span className="buying-power-amount">{formatCurrency(portfolio.cashBalance)}</span>
                </div>
            </div>

            {/* Holdings */}
            <div className="panel mt-1">
                <h3>Holdings</h3>
                {portfolio.holdings.length === 0 ? (
                    <p className="text-secondary empty-state">
                        No holdings yet. Search for a stock to start trading!
                    </p>
                ) : (
                    <div className="holdings-list">
                        {portfolio.holdings.map(h => {
                            const gainPositive = h.totalGainLoss >= 0;
                            return (
                                <div
                                    key={h.symbol}
                                    className="holding-row"
                                    onClick={() => navigate(`/search?symbol=${h.symbol}`)}
                                >
                                    <div className="holding-left">
                                        <span className="holding-symbol">{h.symbol}</span>
                                        <span className="holding-shares text-secondary">
                                            {h.quantity} share{h.quantity !== 1 ? 's' : ''}
                                        </span>
                                    </div>
                                    <div className="holding-right">
                                        <span className="holding-market-value">
                                            {formatCurrency(h.marketValue)}
                                        </span>
                                        <span className={`holding-gain ${gainPositive ? 'positive' : 'negative'}`}>
                                            {gainPositive ? '+' : '-'}{formatCurrency(h.totalGainLoss)} ({gainPositive ? '+' : '-'}{Math.abs(h.totalGainLossPercent).toFixed(2)}%)
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* Recent Trades */}
            <div className="panel mt-1">
                <h3>Recent Trades</h3>
                {trades.length === 0 ? (
                    <p className="text-secondary empty-state">No trades yet.</p>
                ) : (
                    <div className="trade-history-list">
                        {trades.slice(0, 10).map(t => (
                            <div key={t.id} className="trade-history-item">
                                <div className="trade-history-left">
                                    <span className={`trade-history-type ${t.type === 'BUY' ? 'buy' : 'sell'}`}>
                                        {t.type}
                                    </span>
                                    <span className="trade-history-symbol">{t.symbol}</span>
                                    <span className="text-secondary">
                                        {t.quantity} @ ${t.pricePerShare.toFixed(2)}
                                    </span>
                                </div>
                                <div className="trade-history-right">
                                    <span>{formatCurrency(t.totalCost)}</span>
                                    <span className="text-secondary">{formatDate(t.executedAt)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
