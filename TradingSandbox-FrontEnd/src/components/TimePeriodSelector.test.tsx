import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TimePeriodSelector from './TimePeriodSelector';

describe('TimePeriodSelector', () => {
    it('renders all four period buttons', () => {
        render(
            <TimePeriodSelector
                selectedPeriod="1M"
                onPeriodChange={() => {}}
            />
        );

        expect(screen.getByText('1D')).toBeInTheDocument();
        expect(screen.getByText('1W')).toBeInTheDocument();
        expect(screen.getByText('1M')).toBeInTheDocument();
        expect(screen.getByText('3M')).toBeInTheDocument();
    });

    it('applies active class to selected period', () => {
        render(
            <TimePeriodSelector
                selectedPeriod="1W"
                onPeriodChange={() => {}}
            />
        );

        const oneWeekBtn = screen.getByText('1W');
        expect(oneWeekBtn.className).toContain('active');

        const oneDayBtn = screen.getByText('1D');
        expect(oneDayBtn.className).not.toContain('active');
    });

    it('calls onPeriodChange when button clicked', async () => {
        const onChange = vi.fn();
        render(
            <TimePeriodSelector
                selectedPeriod="1M"
                onPeriodChange={onChange}
            />
        );

        await userEvent.click(screen.getByText('1D'));
        expect(onChange).toHaveBeenCalledWith('1D');

        await userEvent.click(screen.getByText('3M'));
        expect(onChange).toHaveBeenCalledWith('3M');
    });

    it('disables all buttons when disabled prop is true', () => {
        render(
            <TimePeriodSelector
                selectedPeriod="1M"
                onPeriodChange={() => {}}
                disabled={true}
            />
        );

        const buttons = screen.getAllByRole('button');
        buttons.forEach((btn) => {
            expect(btn).toBeDisabled();
        });
    });

    it('buttons are enabled when disabled prop is false', () => {
        render(
            <TimePeriodSelector
                selectedPeriod="1M"
                onPeriodChange={() => {}}
                disabled={false}
            />
        );

        const buttons = screen.getAllByRole('button');
        buttons.forEach((btn) => {
            expect(btn).not.toBeDisabled();
        });
    });

    it('does not call onPeriodChange when disabled', async () => {
        const onChange = vi.fn();
        render(
            <TimePeriodSelector
                selectedPeriod="1M"
                onPeriodChange={onChange}
                disabled={true}
            />
        );

        await userEvent.click(screen.getByText('1D'));
        expect(onChange).not.toHaveBeenCalled();
    });
});
