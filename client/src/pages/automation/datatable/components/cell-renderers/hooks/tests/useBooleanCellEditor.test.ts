import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useBooleanCellEditor from '../useBooleanCellEditor';

describe('useBooleanCellEditor', () => {
    const mockSetLocalRows = vi.fn();
    const mockMutate = vi.fn();

    const defaultProps = {
        columnName: 'isActive',
        environmentId: 'env-123',
        row: {id: 'row-1', isActive: false},
        setLocalRows: mockSetLocalRows,
        tableId: 'table-123',
        updateRowMutation: {mutate: mockMutate},
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

        it('calls mutation with correct parameters', () => {
            const {result} = renderHook(() => useBooleanCellEditor(defaultProps));

            act(() => {
                result.current.handleToggle(true);
            });

            expect(mockMutate).toHaveBeenCalledTimes(1);
            expect(mockMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: 'env-123',
                    id: 'row-1',
                    tableId: 'table-123',
                    values: {isActive: true},
                },
            });
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

            expect(mockMutate).toHaveBeenCalledWith({
                input: {
                    environmentId: 'env-123',
                    id: 'row-1',
                    tableId: 'table-123',
                    values: {isActive: false},
                },
            });
        });
    });
});
