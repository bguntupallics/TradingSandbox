import { useState, useEffect } from 'react';
import { fetchWithJwt, fetchPricesByPeriod } from '../services/api';
import type { TimePeriod, PriceData } from '../services/api';
import TimePeriodSelector from '../components/TimePeriodSelector';
import StockSearchInput from '../components/StockSearchInput';
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
    const [stockName, setStockName] = useState<string>('');
    const [priceData, setPriceData] = useState<PriceData[]>([]);
    const [selectedPeriod, setSelectedPeriod] = useState<TimePeriod>('1D');
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

    const handleSearch = async (sym: string, name?: string) => {
        setError(null);
        setPriceData([]);
        setLatestPrice(null);
        setDisplayedSymbol(sym);
        setStockName(name || '');
        await fetchData(sym, selectedPeriod);
    };

    const handleError = (errorMessage: string) => {
        setError(errorMessage);
        setPriceData([]);
        setLatestPrice(null);
    };

    const handlePeriodChange = (period: TimePeriod) => {
        setSelectedPeriod(period);
        // Data fetching is handled by useEffect
    };

    // Compute Y-axis domain with padding so the line doesn't hug edges
    const prices = priceData.map(d => d.closingPrice);
    const minPrice = Math.min(...prices);
    const maxPrice = Math.max(...prices);
    const pricePadding = (maxPrice - minPrice) * 0.05 || 1;
    const yDomain: [number, number] = [
        Math.floor((minPrice - pricePadding) * 100) / 100,
        Math.ceil((maxPrice + pricePadding) * 100) / 100,
    ];

    // Choose how many X-axis labels to show based on data length
    const xTickInterval = priceData.length <= 7
        ? 0
        : Math.ceil(priceData.length / 6) - 1;

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

            <StockSearchInput
                value={symbol}
                onChange={setSymbol}
                onSearch={handleSearch}
                onError={handleError}
                disabled={loading}
                placeholder="Search stocks (e.g. NVDA, AAPL)"
            />

            {error && <p className="error">{error}</p>}

            {/* Return panel: only when we have data */}
            {priceData.length > 0 && (
                <div className="price-panel return-panel">
                    <h2>{displayedSymbol}{stockName && <span className="stock-name"> - {stockName}</span>}:</h2>
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
                    <ResponsiveContainer width="100%" height={350}>
                        <LineChart
                            data={priceData}
                            margin={{ top: 10, right: 20, bottom: 20, left: 10 }}
                        >
                            <CartesianGrid
                                strokeDasharray="3 3"
                                stroke="var(--input-border)"
                                strokeOpacity={0.4}
                                vertical={false}
                            />
                            <ReferenceLine
                                y={firstPrice}
                                stroke="var(--control-text)"
                                strokeWidth={1}
                                strokeDasharray="4 4"
                                strokeOpacity={0.5}
                            />
                            <XAxis
                                dataKey="dateLabel"
                                tick={{ fill: 'var(--text-secondary)', fontSize: 12 }}
                                interval={xTickInterval}
                                axisLine={{ stroke: 'var(--input-border)' }}
                                tickLine={false}
                                tickMargin={8}
                            />
                            <YAxis
                                tick={{ fill: 'var(--text-secondary)', fontSize: 12 }}
                                domain={yDomain}
                                tickCount={6}
                                width={70}
                                axisLine={false}
                                tickLine={false}
                                tickFormatter={(price: number) =>
                                    `$${price.toFixed(2)}`
                                }
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: 'var(--bg-panel)',
                                    border: '1px solid var(--input-border)',
                                    borderRadius: '0.5rem',
                                    fontSize: '0.875rem',
                                }}
                                labelFormatter={(label) => `${label}`}
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
