import { useState, useEffect } from 'react';
import { executeTrade, fetchMarketStatus, fetchWithJwt } from '../services/api';
import type { TradeResult } from '../services/api';
import '../styles/global.css';

interface TradePanelProps {
    symbol: string;
    latestPrice: number | null;
    onTradeComplete: () => void;
}

export default function TradePanel({ symbol, latestPrice, onTradeComplete }: TradePanelProps) {
    const [tradeType, setTradeType] = useState<'BUY' | 'SELL'>('BUY');
    const [quantity, setQuantity] = useState<string>('');
    const [marketOpen, setMarketOpen] = useState<boolean | null>(null);
    const [balance, setBalance] = useState<number | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showConfirmation, setShowConfirmation] = useState(false);
    const [result, setResult] = useState<TradeResult | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchMarketStatus()
            .then(status => setMarketOpen(status.open))
            .catch(() => setMarketOpen(null));

        fetchWithJwt<number>('/api/account/balance')
            .then(setBalance)
            .catch(() => {});
    }, []);

    // Reset state when symbol changes
    useEffect(() => {
        setQuantity('');
        setShowConfirmation(false);
        setResult(null);
        setError(null);
    }, [symbol]);

    const parsedQuantity = parseFloat(quantity);
    const isValidQuantity = !isNaN(parsedQuantity) && parsedQuantity >= 0.01;
    const estimatedTotal = isValidQuantity && latestPrice ? parsedQuantity * latestPrice : 0;

    const handleQuantityChange = (value: string) => {
        // Allow up to 2 decimal places
        if (value === '' || /^\d*\.?\d{0,2}$/.test(value)) {
            setQuantity(value);
            setShowConfirmation(false);
            setResult(null);
            setError(null);
        }
    };

    const handleReview = () => {
        if (!isValidQuantity) return;
        setError(null);
        setResult(null);
        setShowConfirmation(true);
    };

    const handleSubmit = async () => {
        if (!isValidQuantity) return;
        setIsSubmitting(true);
        setError(null);

        try {
            const tradeResult = await executeTrade({
                symbol,
                quantity: parsedQuantity,
                type: tradeType,
            });
            setResult(tradeResult);
            setBalance(tradeResult.remainingCashBalance);
            setShowConfirmation(false);
            setQuantity('');
            onTradeComplete();
        } catch (err) {
            setError((err as Error).message);
            setShowConfirmation(false);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleCancel = () => {
        setShowConfirmation(false);
    };

    return (
        <div className="trade-panel panel">
            <h3>Trade {symbol}</h3>

            {marketOpen === false && (
                <div className="market-closed-banner">
                    Market is currently closed. Orders cannot be placed.
                </div>
            )}

            <div className="trade-type-toggle">
                <button
                    className={`trade-type-btn buy ${tradeType === 'BUY' ? 'active' : ''}`}
                    onClick={() => { setTradeType('BUY'); setShowConfirmation(false); setResult(null); setError(null); }}
                >
                    Buy
                </button>
                <button
                    className={`trade-type-btn sell ${tradeType === 'SELL' ? 'active' : ''}`}
                    onClick={() => { setTradeType('SELL'); setShowConfirmation(false); setResult(null); setError(null); }}
                >
                    Sell
                </button>
            </div>

            <div className="trade-form">
                <label className="trade-label">
                    Shares
                    <input
                        type="text"
                        inputMode="decimal"
                        className="trade-input"
                        placeholder="0.00"
                        value={quantity}
                        onChange={e => handleQuantityChange(e.target.value)}
                        disabled={marketOpen === false || isSubmitting}
                    />
                </label>

                {latestPrice !== null && (
                    <div className="trade-price-info">
                        <span className="text-secondary">Market Price</span>
                        <span>${latestPrice.toFixed(2)}</span>
                    </div>
                )}

                {isValidQuantity && estimatedTotal > 0 && (
                    <div className="trade-estimate">
                        <span className="text-secondary">Estimated {tradeType === 'BUY' ? 'Cost' : 'Credit'}</span>
                        <span>${estimatedTotal.toFixed(2)}</span>
                    </div>
                )}

                {balance !== null && (
                    <div className="trade-balance-info">
                        <span className="text-secondary">Buying Power</span>
                        <span>${balance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                )}
            </div>

            {!showConfirmation && !result && (
                <button
                    className={`trade-submit-btn ${tradeType === 'BUY' ? 'buy' : 'sell'}`}
                    onClick={handleReview}
                    disabled={!isValidQuantity || marketOpen === false || isSubmitting}
                >
                    Review {tradeType === 'BUY' ? 'Buy' : 'Sell'} Order
                </button>
            )}

            {showConfirmation && (
                <div className="trade-confirmation">
                    <p>
                        {tradeType === 'BUY' ? 'Buy' : 'Sell'}{' '}
                        <strong>{parsedQuantity}</strong> share{parsedQuantity !== 1 ? 's' : ''} of{' '}
                        <strong>{symbol}</strong> at ~${latestPrice?.toFixed(2)} per share
                    </p>
                    <p className="trade-confirm-total">
                        Total: <strong>${estimatedTotal.toFixed(2)}</strong>
                    </p>
                    <div className="trade-confirm-buttons">
                        <button
                            className={`trade-submit-btn ${tradeType === 'BUY' ? 'buy' : 'sell'}`}
                            onClick={handleSubmit}
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Placing Order...' : `Confirm ${tradeType === 'BUY' ? 'Buy' : 'Sell'}`}
                        </button>
                        <button className="trade-cancel-btn" onClick={handleCancel} disabled={isSubmitting}>
                            Cancel
                        </button>
                    </div>
                </div>
            )}

            {result && (
                <div className="trade-success">
                    <p>Order filled!</p>
                    <p>
                        {result.type === 'BUY' ? 'Bought' : 'Sold'}{' '}
                        {result.quantity} share{result.quantity !== 1 ? 's' : ''} of{' '}
                        {result.symbol} at ${result.pricePerShare.toFixed(2)}
                    </p>
                    <p className="text-secondary">
                        Total: ${result.totalCost.toFixed(2)} | Remaining Balance: $
                        {result.remainingCashBalance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </p>
                </div>
            )}

            {error && <div className="trade-error">{error}</div>}
        </div>
    );
}
