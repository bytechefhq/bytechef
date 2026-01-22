import {act, renderHook} from '@testing-library/react';
import {type KeyboardEvent, type MouseEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useRowIdCell from '../useRowIdCell';

describe('useRowSelectionCell', () => {
    const mockOnSelectedRowsChange = vi.fn();
    const mockSetHoveredRowId = vi.fn();

    const defaultProps = {
        onSelectedRowsChange: mockOnSelectedRowsChange,
        rowId: 'row-1',
        selectedRows: new Set<string>(),
        setHoveredRowId: mockSetHoveredRowId,
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns isSelected as false when row is not selected', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));

            expect(result.current.isSelected).toBe(false);
        });

        it('returns isSelected as true when row is selected', () => {
            const props = {
                ...defaultProps,
                selectedRows: new Set(['row-1']),
            };

            const {result} = renderHook(() => useRowIdCell(props));

            expect(result.current.isSelected).toBe(true);
        });
    });

    describe('handleToggleRow', () => {
        it('adds row to selection when not selected', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));

            act(() => {
                result.current.handleToggleRow();
            });

            expect(mockOnSelectedRowsChange).toHaveBeenCalledTimes(1);
            const newSelection = mockOnSelectedRowsChange.mock.calls[0][0];

            expect(newSelection.has('row-1')).toBe(true);
        });

        it('removes row from selection when selected', () => {
            const props = {
                ...defaultProps,
                selectedRows: new Set(['row-1', 'row-2']),
            };

            const {result} = renderHook(() => useRowIdCell(props));

            act(() => {
                result.current.handleToggleRow();
            });

            const newSelection = mockOnSelectedRowsChange.mock.calls[0][0];

            expect(newSelection.has('row-1')).toBe(false);
            expect(newSelection.has('row-2')).toBe(true);
        });

        it('preserves other selections when toggling', () => {
            const props = {
                ...defaultProps,
                selectedRows: new Set(['row-2', 'row-3']),
            };

            const {result} = renderHook(() => useRowIdCell(props));

            act(() => {
                result.current.handleToggleRow();
            });

            const newSelection = mockOnSelectedRowsChange.mock.calls[0][0];

            expect(newSelection.has('row-1')).toBe(true);
            expect(newSelection.has('row-2')).toBe(true);
            expect(newSelection.has('row-3')).toBe(true);
        });
    });

    describe('handleKeyDown', () => {
        it('toggles selection on Enter key', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));
            const event = {
                key: 'Enter',
                preventDefault: vi.fn(),
            } as unknown as KeyboardEvent<HTMLDivElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(event.preventDefault).toHaveBeenCalled();
            expect(mockOnSelectedRowsChange).toHaveBeenCalled();
        });

        it('toggles selection on Space key', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));
            const event = {
                key: ' ',
                preventDefault: vi.fn(),
            } as unknown as KeyboardEvent<HTMLDivElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(event.preventDefault).toHaveBeenCalled();
            expect(mockOnSelectedRowsChange).toHaveBeenCalled();
        });

        it('does nothing on other keys', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));
            const event = {
                key: 'Tab',
                preventDefault: vi.fn(),
            } as unknown as KeyboardEvent<HTMLDivElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(event.preventDefault).not.toHaveBeenCalled();
            expect(mockOnSelectedRowsChange).not.toHaveBeenCalled();
        });
    });

    describe('handleCheckedChange', () => {
        it('adds row to selection when checked is true', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));

            act(() => {
                result.current.handleCheckedChange(true);
            });

            const newSelection = mockOnSelectedRowsChange.mock.calls[0][0];

            expect(newSelection.has('row-1')).toBe(true);
        });

        it('removes row from selection when checked is false', () => {
            const props = {
                ...defaultProps,
                selectedRows: new Set(['row-1']),
            };

            const {result} = renderHook(() => useRowIdCell(props));

            act(() => {
                result.current.handleCheckedChange(false);
            });

            const newSelection = mockOnSelectedRowsChange.mock.calls[0][0];

            expect(newSelection.has('row-1')).toBe(false);
        });

        it('removes row from selection when checked is indeterminate', () => {
            const props = {
                ...defaultProps,
                selectedRows: new Set(['row-1']),
            };

            const {result} = renderHook(() => useRowIdCell(props));

            act(() => {
                result.current.handleCheckedChange('indeterminate');
            });

            const newSelection = mockOnSelectedRowsChange.mock.calls[0][0];

            expect(newSelection.has('row-1')).toBe(false);
        });
    });

    describe('handleMouseEnter', () => {
        it('sets hovered row id', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));

            act(() => {
                result.current.handleMouseEnter();
            });

            expect(mockSetHoveredRowId).toHaveBeenCalledWith('row-1');
        });
    });

    describe('handleMouseLeave', () => {
        it('clears hovered row id when current id matches', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));

            act(() => {
                result.current.handleMouseLeave();
            });

            expect(mockSetHoveredRowId).toHaveBeenCalledWith(expect.any(Function));

            const updaterFn = mockSetHoveredRowId.mock.calls[0][0];

            expect(updaterFn('row-1')).toBe(null);
        });

        it('does not clear hovered row id when different id is hovered', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));

            act(() => {
                result.current.handleMouseLeave();
            });

            const updaterFn = mockSetHoveredRowId.mock.calls[0][0];

            expect(updaterFn('row-2')).toBe('row-2');
        });
    });

    describe('handleCheckboxClick', () => {
        it('stops event propagation', () => {
            const {result} = renderHook(() => useRowIdCell(defaultProps));
            const event = {
                stopPropagation: vi.fn(),
            } as unknown as MouseEvent;

            act(() => {
                result.current.handleCheckboxClick(event);
            });

            expect(event.stopPropagation).toHaveBeenCalled();
        });
    });
});
