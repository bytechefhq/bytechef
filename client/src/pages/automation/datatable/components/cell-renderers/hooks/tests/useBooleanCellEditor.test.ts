import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useBooleanCellEditor from '../useBooleanCellEditor';

describe('useBooleanCellEditor', () => {
    const mockOnToggle = vi.fn();
    const mockSetLocalRows = vi.fn();

    const defaultProps = {
        columnName: 'isActive',
        onToggle: mockOnToggle,
        row: {id: 'row-1', isActive: false},
        setLocalRows: mockSetLocalRows,
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns checked as false when cell value is false', () => {
            const {result} = renderHook(() => useBooleanCellEditor(defaultProps));

            expect(result.current.checked).toBe(false);
        });

        it('returns checked as true when cell value is true', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', isActive: true},
            };

            const {result} = renderHook(() => useBooleanCellEditor(props));

            expect(result.current.checked).toBe(true);
        });

        it('returns checked as false when cell value is undefined', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1'},
            };

            const {result} = renderHook(() => useBooleanCellEditor(props));

            expect(result.current.checked).toBe(false);
        });

        it('returns checked as true for truthy non-boolean values', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', isActive: 'yes'},
            };

            const {result} = renderHook(() => useBooleanCellEditor(props));

            expect(result.current.checked).toBe(true);
        });
    });

    describe('handleToggle', () => {
        it('updates local rows optimistically', () => {
            const {result} = renderHook(() => useBooleanCellEditor(defaultProps));

            act(() => {
                result.current.handleToggle(true);
            });

            expect(mockSetLocalRows).toHaveBeenCalledTimes(1);
            expect(mockSetLocalRows).toHaveBeenCalledWith(expect.any(Function));

            const updaterFn = mockSetLocalRows.mock.calls[0][0];
            const previousRows = [{id: 'row-1', isActive: false}];
            const updatedRows = updaterFn(previousRows);

            expect(updatedRows).toEqual([{id: 'row-1', isActive: true}]);
        });

        it('does not update other rows', () => {
            const {result} = renderHook(() => useBooleanCellEditor(defaultProps));

            act(() => {
                result.current.handleToggle(true);
            });

            const updaterFn = mockSetLocalRows.mock.calls[0][0];
            const previousRows = [
                {id: 'row-1', isActive: false},
                {id: 'row-2', isActive: true},
            ];
            const updatedRows = updaterFn(previousRows);

            expect(updatedRows).toEqual([
                {id: 'row-1', isActive: true},
                {id: 'row-2', isActive: true},
            ]);
        });

        it('calls onToggle with correct parameters', () => {
            const {result} = renderHook(() => useBooleanCellEditor(defaultProps));

            act(() => {
                result.current.handleToggle(true);
            });

            expect(mockOnToggle).toHaveBeenCalledTimes(1);
            expect(mockOnToggle).toHaveBeenCalledWith('row-1', 'isActive', true);
        });

        it('toggles from true to false', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1', isActive: true},
            };

            const {result} = renderHook(() => useBooleanCellEditor(props));

            act(() => {
                result.current.handleToggle(false);
            });

            expect(mockOnToggle).toHaveBeenCalledWith('row-1', 'isActive', false);
        });
    });
});
