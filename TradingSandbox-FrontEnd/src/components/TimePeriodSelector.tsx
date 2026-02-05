import type { TimePeriod } from '../services/api';
import '../styles/global.css';

const TIME_PERIODS: { label: string; period: TimePeriod }[] = [
    { label: '1D', period: '1D' },
    { label: '1W', period: '1W' },
    { label: '1M', period: '1M' },
    { label: '3M', period: '3M' },
];

interface Props {
    selectedPeriod: TimePeriod;
    onPeriodChange: (period: TimePeriod) => void;
    disabled?: boolean;
}

export default function TimePeriodSelector({
    selectedPeriod,
    onPeriodChange,
    disabled,
}: Props) {
    return (
        <div className="time-period-selector">
            {TIME_PERIODS.map(({ label, period }) => (
                <button
                    key={period}
                    className={`period-btn ${selectedPeriod === period ? 'active' : ''}`}
                    onClick={() => onPeriodChange(period)}
                    disabled={disabled}
                >
                    {label}
                </button>
            ))}
        </div>
    );
}
