import {act, renderHook} from '@testing-library/react';
import {type ChangeEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDateTimeCellEditor, {formatDateTimeValue, parseDateTime} from '../useDateTimeCellEditor';

describe('useDateTimeCellEditor', () => {
    const mockOnRowChange = vi.fn();

    const defaultProps = {
        columnName: 'updatedAt',
        onRowChange: mockOnRowChange,
        row: {id: 'row-1', updatedAt: '2024-01-15 14:30:00'},
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns popover open by default', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            expect(result.current.isPopoverOpen).toBe(true);
        });

        it('parses initial date and time from row', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            expect(result.current.selectedDate.getFullYear()).toBe(2024);
            expect(result.current.selectedDate.getMonth()).toBe(0);
            expect(result.current.selectedDate.getDate()).toBe(15);
            expect(result.current.selectedDate.getHours()).toBe(14);
            expect(result.current.selectedDate.getMinutes()).toBe(30);
        });

        it('uses current date when cell value is empty', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', updatedAt: ''},
            };

            const {result} = renderHook(() => useDateTimeCellEditor(props));
            const now = new Date();

            expect(result.current.selectedDate.getFullYear()).toBe(now.getFullYear());
        });

        it('returns formatted hours with leading zero', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', updatedAt: '2024-01-15 09:05:00'},
            };

            const {result} = renderHook(() => useDateTimeCellEditor(props));

            expect(result.current.formattedHours).toBe('09');
            expect(result.current.formattedMinutes).toBe('05');
        });

        it('returns display text in correct format', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            expect(result.current.displayText).toBe('2024-01-15 14:30');
        });
    });

    describe('handleDateSelect', () => {
        it('updates date portion while preserving time', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));
            const newDate = new Date(2024, 5, 20);

            act(() => {
                result.current.handleDateSelect(newDate);
            });

            expect(result.current.selectedDate.getFullYear()).toBe(2024);
            expect(result.current.selectedDate.getMonth()).toBe(5);
            expect(result.current.selectedDate.getDate()).toBe(20);
            expect(result.current.selectedDate.getHours()).toBe(14);
            expect(result.current.selectedDate.getMinutes()).toBe(30);
        });

        it('does nothing when date is undefined', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));
            const originalDate = result.current.selectedDate;

            act(() => {
                result.current.handleDateSelect(undefined);
            });

            expect(result.current.selectedDate.getTime()).toBe(originalDate.getTime());
        });
    });

    describe('handleTimeChange', () => {
        it('updates time from input', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));
            const event = {target: {value: '16:45'}} as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleTimeChange(event);
            });

            expect(result.current.selectedDate.getHours()).toBe(16);
            expect(result.current.selectedDate.getMinutes()).toBe(45);
        });

        it('clamps hours to valid range', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));
            const event = {target: {value: '25:30'}} as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleTimeChange(event);
            });

            expect(result.current.selectedDate.getHours()).toBe(23);
        });

        it('clamps minutes to valid range', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));
            const event = {target: {value: '12:70'}} as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleTimeChange(event);
            });

            expect(result.current.selectedDate.getMinutes()).toBe(59);
        });
    });

    describe('handleCommit', () => {
        it('calls onRowChange with formatted datetime', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            act(() => {
                result.current.handleCommit();
            });

            expect(mockOnRowChange).toHaveBeenCalledWith(
                expect.objectContaining({
                    id: 'row-1',
                    updatedAt: expect.stringMatching(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/),
                }),
                true
            );
        });

        it('closes popover after commit', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            act(() => {
                result.current.handleCommit();
            });

            expect(result.current.isPopoverOpen).toBe(false);
        });
    });

    describe('handleOpenChange', () => {
        it('commits when closing popover', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(mockOnRowChange).toHaveBeenCalled();
            expect(result.current.isPopoverOpen).toBe(false);
        });

        it('does not commit when opening popover', () => {
            const {result} = renderHook(() => useDateTimeCellEditor(defaultProps));

            act(() => {
                result.current.handleOpenChange(false);
            });

            vi.clearAllMocks();

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
            expect(result.current.isPopoverOpen).toBe(true);
        });
    });
});

describe('parseDateTime', () => {
    it('parses valid datetime string', () => {
        const result = parseDateTime('2024-01-15 14:30:00');

        expect(result).toBeInstanceOf(Date);
        expect(result?.getFullYear()).toBe(2024);
        expect(result?.getHours()).toBe(14);
    });

    it('returns undefined for empty value', () => {
        expect(parseDateTime('')).toBeUndefined();
        expect(parseDateTime(null)).toBeUndefined();
        expect(parseDateTime(undefined)).toBeUndefined();
    });
});

describe('formatDateTimeValue', () => {
    it('formats valid datetime string', () => {
        expect(formatDateTimeValue('2024-01-15 14:30:00')).toBe('2024-01-15 14:30');
    });

    it('returns empty string for empty value', () => {
        expect(formatDateTimeValue('')).toBe('');
        expect(formatDateTimeValue(null)).toBe('');
    });
});
