import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentChunkListSelectionBar from '../useKnowledgeBaseDocumentChunkListSelectionBar';

const hoisted = vi.hoisted(() => {
    return {
        deleteDialogStoreState: {
            setChunkIdsToDelete: vi.fn(),
        },
        selectionStoreState: {
            clearSelection: vi.fn(),
            selectedChunks: [] as string[],
        },
    };
});

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore', () => ({
    useKnowledgeBaseDocumentChunkSelectionStore: vi.fn(() => ({
        clearSelection: () => {
            hoisted.selectionStoreState.selectedChunks = [];
            hoisted.selectionStoreState.clearSelection();
        },
        selectedChunks: hoisted.selectionStoreState.selectedChunks,
    })),
}));

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkDeleteDialogStore', () => ({
    useKnowledgeBaseDocumentChunkDeleteDialogStore: vi.fn(() => ({
        setChunkIdsToDelete: hoisted.deleteDialogStoreState.setChunkIdsToDelete,
    })),
}));

describe('useKnowledgeBaseDocumentChunkListSelectionBar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.selectionStoreState.selectedChunks = [];
    });

    describe('initial state', () => {
        it('returns no selection when empty', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.hasSelection).toBe(false);
            expect(result.current.selectedCount).toBe(0);
        });

        it('returns selection when chunks are selected', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1', 'chunk-2'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.hasSelection).toBe(true);
            expect(result.current.selectedCount).toBe(2);
        });
    });

    describe('hasSelection', () => {
        it('returns false when no chunks are selected', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.hasSelection).toBe(false);
        });

        it('returns true when one chunk is selected', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.hasSelection).toBe(true);
        });

        it('returns true when multiple chunks are selected', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1', 'chunk-2', 'chunk-3'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.hasSelection).toBe(true);
        });
    });

    describe('selectedCount', () => {
        it('returns 0 when no chunks are selected', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.selectedCount).toBe(0);
        });

        it('returns correct count for single selection', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.selectedCount).toBe(1);
        });

        it('returns correct count for multiple selections', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1', 'chunk-2', 'chunk-3', 'chunk-4'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(result.current.selectedCount).toBe(4);
        });
    });

    describe('handleClearSelection', () => {
        it('clears all selections', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1', 'chunk-2'];

            const {rerender, result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            act(() => {
                result.current.handleClearSelection();
            });

            rerender();

            expect(hoisted.selectionStoreState.clearSelection).toHaveBeenCalled();
            expect(result.current.hasSelection).toBe(false);
            expect(result.current.selectedCount).toBe(0);
        });
    });

    describe('handleDeleteSelected', () => {
        it('sets selected chunks for deletion', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1', 'chunk-2', 'chunk-3'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            act(() => {
                result.current.handleDeleteSelected();
            });

            expect(hoisted.deleteDialogStoreState.setChunkIdsToDelete).toHaveBeenCalledWith([
                'chunk-1',
                'chunk-2',
                'chunk-3',
            ]);
        });

        it('opens delete dialog for single selection', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            act(() => {
                result.current.handleDeleteSelected();
            });

            expect(hoisted.deleteDialogStoreState.setChunkIdsToDelete).toHaveBeenCalledWith(['chunk-1']);
        });

        it('calls setChunkIdsToDelete with empty array when no selection', () => {
            hoisted.selectionStoreState.selectedChunks = [];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            act(() => {
                result.current.handleDeleteSelected();
            });

            expect(hoisted.deleteDialogStoreState.setChunkIdsToDelete).toHaveBeenCalledWith([]);
        });
    });

    describe('return values', () => {
        it('returns all expected properties', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListSelectionBar());

            expect(typeof result.current.handleClearSelection).toBe('function');
            expect(typeof result.current.handleDeleteSelected).toBe('function');
            expect(typeof result.current.hasSelection).toBe('boolean');
            expect(typeof result.current.selectedCount).toBe('number');
        });
    });
});
