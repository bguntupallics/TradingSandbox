import { useState } from 'react';
import { fetchWithJwt } from '../services/api.ts';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    ReferenceLine
} from 'recharts';
import '../styles/global.css';

interface DailyPrice {
    symbol: string;
    date: string;
    closingPrice: number;
}

interface TradeResponse {
    symbol: string;
    price: number;
}

export default function SearchPage() {
    const [symbol, setSymbol] = useState<string>('');
    const [displayedSymbol, setDisplayedSymbol] = useState<string>('');
    const [monthlyData, setMonthlyData] = useState<DailyPrice[]>([]);
    const [latestPrice, setLatestPrice] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(false);

    const handleSearch = async () => {
        setError(null);
        setMonthlyData([]);
        setLatestPrice(null);

        if (!symbol) {
            setError('Please enter a ticker symbol.');
            return;
        }

        setLoading(true);
        try {
            // 1) Fetch last-month prices
            const bars: DailyPrice[] = await fetchWithJwt(`/api/prices/${symbol}/last-month`);

            if (bars.length === 0) {
                setError(`No price data found for ${symbol}`);
            } else {
                setMonthlyData(bars);
                setDisplayedSymbol(symbol);
            }

            // 2) Fetch the latest price
            const trade: TradeResponse = await fetchWithJwt(`/api/prices/${symbol}/latest-trade`);

            const tradeDate = new Date().toISOString().split('T')[0];
            const combined: DailyPrice[] = [
                ...bars,
                {
                    symbol,
                    date: tradeDate,
                    closingPrice: trade.price
                }
            ];

            setMonthlyData(combined);
            setLatestPrice(trade.price);
            setDisplayedSymbol(symbol);
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    // Compute dollar and percent return
    const firstPrice = monthlyData.length ? monthlyData[0].closingPrice : 0;
    const lastPrice = monthlyData.length ? monthlyData[monthlyData.length - 1].closingPrice : 0;
    const dollarReturn = lastPrice - firstPrice;
    const percentReturn = firstPrice !== 0 ? (dollarReturn / firstPrice) * 100 : 0;

    return (
        <div className="container dashboard">
            <header>
                <h1>Search</h1>
            </header>

            <div className="search-bar">
                <input
                    className="ticker-input"
                    type="text"
                    placeholder="e.g. NVDA"
                    value={symbol}
                    onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                />
                <button
                    className="btn search-btn"
                    onClick={handleSearch}
                    disabled={loading}
                >
                    {loading ? 'Searching…' : 'Search'}
                </button>
            </div>

            {error && <p className="error">{error}</p>}

            {/* Return panel: only when we have data */}
            {monthlyData.length > 0 && (
                <div className="price-panel return-panel">
                    <h2>{displayedSymbol}:</h2>
                    <div className="return-values">
                        {latestPrice !== null && (
                            <h2 className="latest-price">
                                ${latestPrice.toFixed(2)}
                            </h2>
                        )}
                        <h2 className={dollarReturn >= 0 ? 'return-value positive' : 'return-value negative'}>
                            ${Math.abs(dollarReturn).toFixed(2)} {Math.abs(percentReturn).toFixed(2)}%
                        </h2>
                    </div>
                </div>
            )}

            {/* Chart for last-month data */}
            {monthlyData.length > 0 && (
                <div className="price-panel chart-panel">
                    <h2>Last Month</h2>
                    <ResponsiveContainer width="100%" height={300}>
                        <LineChart data={monthlyData}>
                            <CartesianGrid
                                strokeDasharray="3 3"
                                stroke="var(--input-border)"
                            />
                            <ReferenceLine
                                y={firstPrice}
                                stroke="var(--control-text)"
                                strokeWidth={1}
                            />
                            <XAxis
                                dataKey="date"
                                tick={{ fill: 'var(--text-secondary)' }}
                                tickFormatter={(dateStr: string) => {
                                    const d = new Date(dateStr);
                                    const m = d.getMonth() + 1;
                                    const day = d.getDate();
                                    const y = d.getFullYear();
                                    return `${m}/${day}/${y}`;
                                }}
                            />
                            <YAxis
                                tick={{ fill: 'var(--text-secondary)' }}
                                domain={['dataMin', 'dataMax']}
                                tickFormatter={(price: number) =>
                                    `$${price.toFixed(2)}`
                                }
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: 'var(--bg-panel)',
                                    border: 'none',
                                    borderRadius: '0.5rem'
                                }}
                                labelFormatter={(label) => {
                                    const d = new Date(label);
                                    const m = d.getMonth() + 1;
                                    const day = d.getDate();
                                    const y = d.getFullYear();
                                    return `Date: ${m}/${day}/${y}`;
                                }}
                                formatter={(value: number) => [
                                    `$${value.toFixed(2)}`,
                                    'Close'
                                ]}
                            />
                            <Line
                                type="linear"
                                dataKey="closingPrice"
                                stroke="var(--control-text)"
                                strokeWidth={2}
                                dot={false}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>
            )}
        </div>
    );
};
