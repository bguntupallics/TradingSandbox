import { useState, useEffect } from 'react';
import { fetchWithJwt, fetchPricesByPeriod } from '../services/api';
import type { TimePeriod, PriceData } from '../services/api';
import TimePeriodSelector from '../components/TimePeriodSelector';
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

interface TradeResponse {
    symbol: string;
    price: number;
}

const PERIOD_LABELS: Record<TimePeriod, string> = {
    '1D': 'Today',
    '1W': 'Past Week',
    '1M': 'Past Month',
    '3M': 'Past 3 Months',
};

export default function SearchPage() {
    const [symbol, setSymbol] = useState<string>('');
    const [displayedSymbol, setDisplayedSymbol] = useState<string>('');
    const [priceData, setPriceData] = useState<PriceData[]>([]);
    const [selectedPeriod, setSelectedPeriod] = useState<TimePeriod>('1M');
    const [latestPrice, setLatestPrice] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(false);

    // Refetch data when period changes (if we have a symbol)
    useEffect(() => {
        if (displayedSymbol) {
            fetchData(displayedSymbol, selectedPeriod);
        }
    }, [selectedPeriod]);

    const fetchData = async (sym: string, period: TimePeriod) => {
        setLoading(true);
        setError(null);
        try {
            // Fetch historical data for the selected period
            const data = await fetchPricesByPeriod(sym, period);

            if (data.length === 0) {
                setError(`No price data found for ${sym}`);
                setPriceData([]);
                return;
            }

            setPriceData(data);

            // Fetch the latest trade price (non-blocking - chart works without it)
            try {
                const trade: TradeResponse = await fetchWithJwt(`/api/prices/${sym}/latest-trade`);
                setLatestPrice(trade.price);
            } catch {
                // Use last price from historical data as fallback
                setLatestPrice(data[data.length - 1]?.closingPrice ?? null);
            }
        } catch (err) {
            setError((err as Error).message);
            setPriceData([]);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async () => {
        setError(null);
        setPriceData([]);
        setLatestPrice(null);

        if (!symbol) {
            setError('Please enter a ticker symbol.');
            return;
        }

        setDisplayedSymbol(symbol);
        await fetchData(symbol, selectedPeriod);
    };

    const handlePeriodChange = (period: TimePeriod) => {
        setSelectedPeriod(period);
        // Data fetching is handled by useEffect
    };

    // Compute dollar and percent return
    const firstPrice = priceData.length ? priceData[0].closingPrice : 0;
    const lastPrice = priceData.length ? priceData[priceData.length - 1].closingPrice : 0;
    const dollarReturn = lastPrice - firstPrice;
    const percentReturn = firstPrice !== 0 ? (dollarReturn / firstPrice) * 100 : 0;
    const isPositive = dollarReturn >= 0;

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
                    onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                />
                <button
                    className="btn search-btn"
                    onClick={handleSearch}
                    disabled={loading}
                >
                    {loading ? 'Searching...' : 'Search'}
                </button>
            </div>

            {error && <p className="error">{error}</p>}

            {/* Return panel: only when we have data */}
            {priceData.length > 0 && (
                <div className="price-panel return-panel">
                    <h2>{displayedSymbol}:</h2>
                    <div className="return-values">
                        {latestPrice !== null && (
                            <h2 className="latest-price">
                                ${latestPrice.toFixed(2)}
                            </h2>
                        )}
                        <h2 className={isPositive ? 'return-value positive' : 'return-value negative'}>
                            {isPositive ? '+' : '-'}${Math.abs(dollarReturn).toFixed(2)} ({Math.abs(percentReturn).toFixed(2)}%)
                        </h2>
                    </div>
                </div>
            )}

            {/* Time period selector */}
            {priceData.length > 0 && (
                <TimePeriodSelector
                    selectedPeriod={selectedPeriod}
                    onPeriodChange={handlePeriodChange}
                    disabled={loading}
                />
            )}

            {/* Chart for selected period */}
            {priceData.length > 0 && (
                <div className="price-panel chart-panel">
                    <h2>{PERIOD_LABELS[selectedPeriod]}</h2>
                    <ResponsiveContainer width="100%" height={300}>
                        <LineChart data={priceData}>
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
                                dataKey="dateLabel"
                                tick={{ fill: 'var(--text-secondary)' }}
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
                                labelFormatter={(label) => `Time: ${label}`}
                                formatter={(value: number) => [
                                    `$${value.toFixed(2)}`,
                                    'Price'
                                ]}
                            />
                            <Line
                                type="linear"
                                dataKey="closingPrice"
                                stroke={isPositive ? '#4caf50' : '#f44336'}
                                strokeWidth={2}
                                dot={false}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>
            )}
        </div>
    );
}
