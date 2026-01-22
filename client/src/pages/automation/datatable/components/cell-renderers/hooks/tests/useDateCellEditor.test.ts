import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDateCellEditor, {formatDateValue, parseDate} from '../useDateCellEditor';

describe('useDateCellEditor', () => {
    const mockOnRowChange = vi.fn();

    const defaultProps = {
        columnName: 'createdAt',
        onRowChange: mockOnRowChange,
        row: {createdAt: '2024-01-15', id: 'row-1'},
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns popover open by default', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));

            expect(result.current.isPopoverOpen).toBe(true);
        });

        it('parses initial date from row', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));

            expect(result.current.initialDate).toBeInstanceOf(Date);
            expect(result.current.initialDate?.getFullYear()).toBe(2024);
            expect(result.current.initialDate?.getMonth()).toBe(0);
            expect(result.current.initialDate?.getDate()).toBe(15);
        });

        it('returns undefined initialDate when cell value is empty', () => {
            const props = {
                ...defaultProps,
                row: {createdAt: '', id: 'row-1'},
            };

            const {result} = renderHook(() => useDateCellEditor(props));

            expect(result.current.initialDate).toBeUndefined();
        });

        it('returns formatted display text', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));

            expect(result.current.displayText).toBe('2024-01-15');
        });

        it('returns empty display text when cell value is empty', () => {
            const props = {
                ...defaultProps,
                row: {createdAt: '', id: 'row-1'},
            };

            const {result} = renderHook(() => useDateCellEditor(props));

            expect(result.current.displayText).toBe('');
        });
    });

    describe('handleDateSelect', () => {
        it('calls onRowChange with formatted date and commits', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));
            const selectedDate = new Date(2024, 5, 20);

            act(() => {
                result.current.handleDateSelect(selectedDate);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({createdAt: '2024-06-20', id: 'row-1'}, true);
        });

        it('closes popover after selection', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));
            const selectedDate = new Date(2024, 5, 20);

            act(() => {
                result.current.handleDateSelect(selectedDate);
            });

            expect(result.current.isPopoverOpen).toBe(false);
        });

        it('does nothing when selectedDate is undefined', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));

            act(() => {
                result.current.handleDateSelect(undefined);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
            expect(result.current.isPopoverOpen).toBe(true);
        });
    });

    describe('setIsPopoverOpen', () => {
        it('allows closing the popover', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));

            act(() => {
                result.current.setIsPopoverOpen(false);
            });

            expect(result.current.isPopoverOpen).toBe(false);
        });

        it('allows reopening the popover', () => {
            const {result} = renderHook(() => useDateCellEditor(defaultProps));

            act(() => {
                result.current.setIsPopoverOpen(false);
            });

            act(() => {
                result.current.setIsPopoverOpen(true);
            });

            expect(result.current.isPopoverOpen).toBe(true);
        });
    });
});

describe('parseDate', () => {
    it('parses valid date string', () => {
        const result = parseDate('2024-01-15');

        expect(result).toBeInstanceOf(Date);
        expect(result?.getFullYear()).toBe(2024);
    });

    it('returns undefined for empty value', () => {
        expect(parseDate('')).toBeUndefined();
        expect(parseDate(null)).toBeUndefined();
        expect(parseDate(undefined)).toBeUndefined();
    });

    it('returns undefined for invalid date string', () => {
        expect(parseDate('invalid-date')).toBeUndefined();
    });
});

describe('formatDateValue', () => {
    it('formats valid date string', () => {
        expect(formatDateValue('2024-01-15')).toBe('2024-01-15');
    });

    it('returns empty string for empty value', () => {
        expect(formatDateValue('')).toBe('');
        expect(formatDateValue(null)).toBe('');
        expect(formatDateValue(undefined)).toBe('');
    });

    it('returns original string for invalid date', () => {
        expect(formatDateValue('invalid')).toBe('');
    });
});
