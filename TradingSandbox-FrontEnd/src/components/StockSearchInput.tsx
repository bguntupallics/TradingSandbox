import { useState, useEffect, useRef } from 'react';
import { searchStocks, validateStock } from '../services/api';
import type { StockSuggestion } from '../services/api';
import '../styles/global.css';

interface Props {
    value: string;
    onChange: (value: string) => void;
    onSearch: (symbol: string, stockName?: string) => void;
    onError: (error: string) => void;
    disabled?: boolean;
    placeholder?: string;
}

export default function StockSearchInput({
    value,
    onChange,
    onSearch,
    onError,
    disabled = false,
    placeholder = 'e.g. NVDA',
}: Props) {
    const [suggestions, setSuggestions] = useState<StockSuggestion[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const [isValidating, setIsValidating] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // Debounced search for suggestions
    const fetchSuggestions = async (query: string) => {
        if (query.length < 1) {
            setSuggestions([]);
            return;
        }

        try {
            const results = await searchStocks(query, 8);
            setSuggestions(results);
            setShowSuggestions(results.length > 0);
            setSelectedIndex(-1);
        } catch {
            setSuggestions([]);
        }
    };

    // Handle input change with debouncing
    useEffect(() => {
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }

        debounceRef.current = setTimeout(() => {
            fetchSuggestions(value);
        }, 300);

        return () => {
            if (debounceRef.current) {
                clearTimeout(debounceRef.current);
            }
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value]);

    // Close suggestions when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setShowSuggestions(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newValue = e.target.value.toUpperCase();
        onChange(newValue);
        onError(''); // Clear any previous error
    };

    const handleSelectSuggestion = (suggestion: StockSuggestion) => {
        onChange(suggestion.symbol);
        setShowSuggestions(false);
        setSuggestions([]);
        onSearch(suggestion.symbol, suggestion.name);
    };

    const handleSubmit = async () => {
        if (!value.trim()) {
            onError('Please enter a ticker symbol.');
            return;
        }

        setShowSuggestions(false);
        setIsValidating(true);

        try {
            const validation = await validateStock(value);

            if (!validation.valid) {
                onError(validation.error || `Stock symbol '${value}' not found. Please check the ticker and try again.`);
                setIsValidating(false);
                return;
            }

            onSearch(value, validation.name);
        } catch (err) {
            onError((err as Error).message || 'Failed to validate stock symbol.');
        } finally {
            setIsValidating(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!showSuggestions || suggestions.length === 0) {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSubmit();
            }
            return;
        }

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setSelectedIndex(prev =>
                    prev < suggestions.length - 1 ? prev + 1 : prev
                );
                break;
            case 'ArrowUp':
                e.preventDefault();
                setSelectedIndex(prev => (prev > 0 ? prev - 1 : -1));
                break;
            case 'Enter':
                e.preventDefault();
                if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
                    handleSelectSuggestion(suggestions[selectedIndex]);
                } else {
                    handleSubmit();
                }
                break;
            case 'Escape':
                setShowSuggestions(false);
                setSelectedIndex(-1);
                break;
        }
    };

    const handleFocus = () => {
        if (suggestions.length > 0) {
            setShowSuggestions(true);
        }
    };

    const isLoading = disabled || isValidating;

    return (
        <div className="stock-search-container" ref={containerRef}>
            <div className="search-bar">
                <input
                    ref={inputRef}
                    className="ticker-input"
                    type="text"
                    placeholder={placeholder}
                    value={value}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    onFocus={handleFocus}
                    disabled={isLoading}
                    autoComplete="off"
                    aria-autocomplete="list"
                    aria-expanded={showSuggestions}
                    aria-controls="stock-suggestions"
                />
                <button
                    className="btn search-btn"
                    onClick={handleSubmit}
                    disabled={isLoading}
                >
                    {isValidating ? 'Validating...' : disabled ? 'Searching...' : 'Search'}
                </button>
            </div>

            {showSuggestions && suggestions.length > 0 && (
                <ul
                    id="stock-suggestions"
                    className="suggestions-dropdown"
                    role="listbox"
                >
                    {suggestions.map((suggestion, index) => (
                        <li
                            key={suggestion.symbol}
                            className={`suggestion-item ${index === selectedIndex ? 'selected' : ''}`}
                            onClick={() => handleSelectSuggestion(suggestion)}
                            onMouseEnter={() => setSelectedIndex(index)}
                            role="option"
                            aria-selected={index === selectedIndex}
                        >
                            <span className="suggestion-symbol">{suggestion.symbol}</span>
                            <span className="suggestion-name">{suggestion.name}</span>
                            <span className="suggestion-exchange">{suggestion.exchange}</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
