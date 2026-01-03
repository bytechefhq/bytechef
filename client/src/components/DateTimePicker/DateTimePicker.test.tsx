import '@testing-library/jest-dom';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {format} from 'date-fns';
import {describe, expect, it, vi} from 'vitest';

import DateTimePicker from './DateTimePicker';

describe('DateTimePicker', () => {
    it('should render the trigger button with placeholder when no value is provided', () => {
        render(<DateTimePicker onChange={vi.fn()} />);

        expect(screen.getByText('Pick a date and time')).toBeInTheDocument();
    });

    it('should render the trigger button with formatted date when value is provided', () => {
        const date = new Date(2023, 11, 25, 10, 30);

        render(<DateTimePicker onChange={vi.fn()} value={date} />);

        expect(screen.getByText(format(date, 'PPP HH:mm'))).toBeInTheDocument();
    });

    it('should open popover and allow date selection', async () => {
        const onChange = vi.fn();
        const initialDate = new Date(2023, 11, 25, 10, 30);

        render(<DateTimePicker onChange={onChange} value={initialDate} />);

        const trigger = screen.getByRole('button');

        await userEvent.click(trigger);

        // Calendar should be visible.
        // We look for a day, e.g., 26th.
        const day26 = screen.getByText('26');

        await userEvent.click(day26);

        expect(onChange).toHaveBeenCalled();

        const calledDate = onChange.mock.calls[0][0];

        expect(calledDate.getDate()).toBe(26);
        // Time should be preserved
        expect(calledDate.getHours()).toBe(10);
        expect(calledDate.getMinutes()).toBe(30);
    });

    it('should allow time change', async () => {
        const onChange = vi.fn();
        const initialDate = new Date(2023, 11, 25, 10, 30);

        render(<DateTimePicker onChange={onChange} value={initialDate} />);

        await userEvent.click(screen.getByRole('button'));

        const timeInput = document.querySelector('input[type="time"]') as HTMLInputElement;

        fireEvent.change(timeInput, {target: {value: '15:45'}});

        expect(onChange).toHaveBeenCalled();

        const calledDate = onChange.mock.calls[0][0];

        expect(calledDate.getHours()).toBe(15);
        expect(calledDate.getMinutes()).toBe(45);
        // Date should be preserved
        expect(calledDate.getDate()).toBe(25);
    });

    it('should ignore invalid time inputs', async () => {
        const onChange = vi.fn();
        const initialDate = new Date(2023, 11, 25, 10, 30);

        render(<DateTimePicker onChange={onChange} value={initialDate} />);

        await userEvent.click(screen.getByRole('button'));

        const timeInput = document.querySelector('input[type="time"]') as HTMLInputElement;

        // Invalid hours
        fireEvent.change(timeInput, {target: {value: '25:00'}});

        expect(onChange).not.toHaveBeenCalled();

        // Invalid minutes
        fireEvent.change(timeInput, {target: {value: '10:60'}});

        expect(onChange).not.toHaveBeenCalled();

        // Non-numeric
        fireEvent.change(timeInput, {target: {value: 'ab:cd'}});

        expect(onChange).not.toHaveBeenCalled();
    });

    it('should handle time change when no initial date is selected', async () => {
        const onChange = vi.fn();

        render(<DateTimePicker onChange={onChange} />);

        await userEvent.click(screen.getByRole('button'));

        const timeInput = document.querySelector('input[type="time"]') as HTMLInputElement;

        fireEvent.change(timeInput, {target: {value: '12:00'}});

        expect(onChange).toHaveBeenCalled();

        const calledDate = onChange.mock.calls[0][0];

        expect(calledDate.getHours()).toBe(12);
        expect(calledDate.getMinutes()).toBe(0);
    });
});
