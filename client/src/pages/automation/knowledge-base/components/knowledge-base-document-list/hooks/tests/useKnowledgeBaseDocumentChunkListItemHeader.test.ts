import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentChunkListItemHeader from '../useKnowledgeBaseDocumentChunkListItemHeader';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            selectedChunks: [] as string[],
            toggleChunkSelection: vi.fn(),
        },
    };
});

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore', () => ({
    useKnowledgeBaseDocumentChunkSelectionStore: vi.fn(() => ({
        selectedChunks: hoisted.storeState.selectedChunks,
        toggleChunkSelection: (chunkId: string) => {
            const index = hoisted.storeState.selectedChunks.indexOf(chunkId);

            if (index > -1) {
                hoisted.storeState.selectedChunks = hoisted.storeState.selectedChunks.filter((id) => id !== chunkId);
            } else {
                hoisted.storeState.selectedChunks = [...hoisted.storeState.selectedChunks, chunkId];
            }

            hoisted.storeState.toggleChunkSelection(chunkId);
        },
    })),
}));

describe('useKnowledgeBaseDocumentChunkListItemHeader', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.selectedChunks = [];
    });

    describe('isSelected', () => {
        it('returns false when chunk is not selected', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'}));

            expect(result.current.isSelected).toBe(false);
        });

        it('returns true when chunk is selected', () => {
            hoisted.storeState.selectedChunks = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'}));

            expect(result.current.isSelected).toBe(true);
        });

        it('returns false when a different chunk is selected', () => {
            hoisted.storeState.selectedChunks = ['chunk-2'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'}));

            expect(result.current.isSelected).toBe(false);
        });

        it('returns true when multiple chunks are selected including this one', () => {
            hoisted.storeState.selectedChunks = ['chunk-1', 'chunk-2', 'chunk-3'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-2'}));

            expect(result.current.isSelected).toBe(true);
        });
    });

    describe('handleSelectionChange', () => {
        it('toggles selection for the chunk', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'}));

            act(() => {
                result.current.handleSelectionChange();
            });

            expect(hoisted.storeState.toggleChunkSelection).toHaveBeenCalledWith('chunk-1');
        });

        it('selects chunk when not selected', () => {
            const {rerender, result} = renderHook(() =>
                useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'})
            );

            expect(result.current.isSelected).toBe(false);

            act(() => {
                result.current.handleSelectionChange();
            });

            rerender();

            expect(result.current.isSelected).toBe(true);
        });

        it('deselects chunk when already selected', () => {
            hoisted.storeState.selectedChunks = ['chunk-1'];

            const {rerender, result} = renderHook(() =>
                useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'})
            );

            expect(result.current.isSelected).toBe(true);

            act(() => {
                result.current.handleSelectionChange();
            });

            rerender();

            expect(result.current.isSelected).toBe(false);
        });

        it('toggles correct chunk when multiple chunks exist', () => {
            hoisted.storeState.selectedChunks = ['chunk-2'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'}));

            act(() => {
                result.current.handleSelectionChange();
            });

            expect(hoisted.storeState.toggleChunkSelection).toHaveBeenCalledWith('chunk-1');
        });
    });

    describe('return values', () => {
        it('returns isSelected and handleSelectionChange', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemHeader({chunkId: 'chunk-1'}));

            expect(typeof result.current.isSelected).toBe('boolean');
            expect(typeof result.current.handleSelectionChange).toBe('function');
        });
    });
});
