import {act, renderHook} from '@testing-library/react';
import {type ChangeEvent, type KeyboardEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useStringCellEditor from '../useStringCellEditor';

describe('useStringCellEditor', () => {
    const mockOnRowChange = vi.fn();

    const defaultProps = {
        columnName: 'description',
        onRowChange: mockOnRowChange,
        row: {description: 'Initial text', id: 'row-1'},
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns open by default', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            expect(result.current.isOpen).toBe(true);
        });

        it('initializes text from row', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            expect(result.current.text).toBe('Initial text');
        });

        it('handles null cell value', () => {
            const props = {
                ...defaultProps,
                row: {description: null, id: 'row-1'},
            };

            const {result} = renderHook(() => useStringCellEditor(props));

            expect(result.current.text).toBe('');
        });

        it('handles undefined cell value', () => {
            const props = {
                ...defaultProps,
                row: {id: 'row-1'},
            };

            const {result} = renderHook(() => useStringCellEditor(props));

            expect(result.current.text).toBe('');
        });
    });

    describe('handleTextChange', () => {
        it('updates text value', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));
            const event = {target: {value: 'New text'}} as ChangeEvent<HTMLTextAreaElement>;

            act(() => {
                result.current.handleTextChange(event);
            });

            expect(result.current.text).toBe('New text');
        });

        it('handles empty text', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));
            const event = {target: {value: ''}} as ChangeEvent<HTMLTextAreaElement>;

            act(() => {
                result.current.handleTextChange(event);
            });

            expect(result.current.text).toBe('');
        });
    });

    describe('handleSave', () => {
        it('calls onRowChange with updated text and commits', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleTextChange({target: {value: 'Updated text'}} as ChangeEvent<HTMLTextAreaElement>);
            });

            act(() => {
                result.current.handleSave();
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({description: 'Updated text', id: 'row-1'}, true);
        });

        it('closes the popover', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleSave();
            });

            expect(result.current.isOpen).toBe(false);
        });
    });

    describe('handleCancel', () => {
        it('calls onRowChange with original row and commits', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleTextChange({target: {value: 'Changed text'}} as ChangeEvent<HTMLTextAreaElement>);
            });

            act(() => {
                result.current.handleCancel();
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({description: 'Initial text', id: 'row-1'}, true);
        });

        it('closes the popover', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleCancel();
            });

            expect(result.current.isOpen).toBe(false);
        });
    });

    describe('handleKeyDown', () => {
        it('saves on Enter key', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));
            const event = {
                key: 'Enter',
                preventDefault: vi.fn(),
                shiftKey: false,
                stopPropagation: vi.fn(),
            } as unknown as KeyboardEvent<HTMLTextAreaElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({description: 'Initial text', id: 'row-1'}, true);
            expect(event.preventDefault).toHaveBeenCalled();
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        it('does not save on Shift+Enter', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));
            const event = {
                key: 'Enter',
                preventDefault: vi.fn(),
                shiftKey: true,
                stopPropagation: vi.fn(),
            } as unknown as KeyboardEvent<HTMLTextAreaElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        it('cancels on Escape key', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleTextChange({target: {value: 'Changed'}} as ChangeEvent<HTMLTextAreaElement>);
            });

            const event = {
                key: 'Escape',
                stopPropagation: vi.fn(),
            } as unknown as KeyboardEvent<HTMLTextAreaElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({description: 'Initial text', id: 'row-1'}, true);
            expect(event.stopPropagation).toHaveBeenCalled();
        });

        it('does nothing on other keys', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));
            const event = {
                key: 'Tab',
            } as unknown as KeyboardEvent<HTMLTextAreaElement>;

            act(() => {
                result.current.handleKeyDown(event);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
            expect(result.current.isOpen).toBe(true);
        });
    });

    describe('handleOpenChange', () => {
        it('calls onRowChange when closing without commit', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(mockOnRowChange).toHaveBeenCalledWith({description: 'Initial text', id: 'row-1'}, true);
        });

        it('does not call onRowChange when closing after save', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleSave();
            });

            vi.clearAllMocks();

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
        });

        it('does not call onRowChange when opening', () => {
            const {result} = renderHook(() => useStringCellEditor(defaultProps));

            act(() => {
                result.current.handleOpenChange(false);
            });

            vi.clearAllMocks();

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(mockOnRowChange).not.toHaveBeenCalled();
        });
    });
});
