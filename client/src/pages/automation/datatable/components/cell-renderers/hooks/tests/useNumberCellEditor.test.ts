import {act, renderHook} from '@testing-library/react';
import {type ChangeEvent, type FocusEvent, type KeyboardEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useNumberCellEditor from '../useNumberCellEditor';

describe('useNumberCellEditor', () => {
    const mockOnRowChange = vi.fn();

    const defaultProps = {
        columnName: 'quantity',
        onRowChange: mockOnRowChange,
        row: {id: 'row-1', quantity: 42},
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns input value as string', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));

            expect(result.current.inputValue).toBe('42');
        });

        it('returns empty string when cell value is null', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', quantity: null},
            };

            const {result} = renderHook(() => useNumberCellEditor(props));

            expect(result.current.inputValue).toBe('');
        });

        it('returns empty string when cell value is undefined', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1'},
            };

            const {result} = renderHook(() => useNumberCellEditor(props));

            expect(result.current.inputValue).toBe('');
        });

        it('handles decimal numbers', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', quantity: 3.14},
            };

            const {result} = renderHook(() => useNumberCellEditor(props));

            expect(result.current.inputValue).toBe('3.14');
        });

        it('handles negative numbers', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', quantity: -10},
            };

            const {result} = renderHook(() => useNumberCellEditor(props));

            expect(result.current.inputValue).toBe('-10');
        });
    });

    describe('handleChange', () => {
        it('updates input value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: '100'}} as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleChange(event);
            });

            expect(result.current.inputValue).toBe('100');
        });

        it('allows non-numeric input temporarily', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: 'abc'}} as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleChange(event);
            });

            expect(result.current.inputValue).toBe('abc');
        });
    });

    describe('handleBlur', () => {
        it('commits valid numeric value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: '100'}} as FocusEvent<HTMLInputElement>;

            act(() => {
                result.current.handleBlur(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({id: 'row-1', quantity: 100}, true);
        });

        it('commits null for empty value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: ''}} as FocusEvent<HTMLInputElement>;

            act(() => {
                result.current.handleBlur(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({id: 'row-1', quantity: null}, true);
        });

        it('commits null for whitespace-only value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: '   '}} as FocusEvent<HTMLInputElement>;

            act(() => {
                result.current.handleBlur(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({id: 'row-1', quantity: null}, true);
        });

        it('does not commit for invalid numeric value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: 'abc'}} as FocusEvent<HTMLInputElement>;

            act(() => {
                result.current.handleBlur(event);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
        });

        it('commits decimal value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: '3.14'}} as FocusEvent<HTMLInputElement>;

            act(() => {
                result.current.handleBlur(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({id: 'row-1', quantity: 3.14}, true);
        });

        it('commits negative value', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));
            const event = {target: {value: '-50'}} as FocusEvent<HTMLInputElement>;

            act(() => {
                result.current.handleBlur(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({id: 'row-1', quantity: -50}, true);
        });
    });

    describe('handleKeyDown', () => {
        it('commits on Enter key', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));

            act(() => {
                result.current.handleChange({target: {value: '100'}} as ChangeEvent<HTMLInputElement>);
            });

            act(() => {
                result.current.handleKeyDown({key: 'Enter'} as KeyboardEvent<HTMLInputElement>);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({id: 'row-1', quantity: 100}, true);
        });

        it('does not commit on other keys', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));

            act(() => {
                result.current.handleKeyDown({key: 'Tab'} as KeyboardEvent<HTMLInputElement>);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
        });

        it('does not commit on Escape key', () => {
            const {result} = renderHook(() => useNumberCellEditor(defaultProps));

            act(() => {
                result.current.handleKeyDown({key: 'Escape'} as KeyboardEvent<HTMLInputElement>);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
        });
    });
});
