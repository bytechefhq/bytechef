import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentChunkList from '../useKnowledgeBaseDocumentChunkList';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            clearSelection: vi.fn(),
            selectedChunks: [] as string[],
            toggleChunkSelection: vi.fn(),
        },
    };
});

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore', () => ({
    useKnowledgeBaseDocumentChunkSelectionStore: vi.fn(() => ({
        clearSelection: () => {
            hoisted.storeState.selectedChunks = [];
            hoisted.storeState.clearSelection();
        },
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

describe('useKnowledgeBaseDocumentChunkList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.selectedChunks = [];
    });

    describe('initial state', () => {
        it('returns empty selection initially', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            expect(result.current.selectedChunks).toEqual([]);
        });
    });

    describe('handleSelectChunk', () => {
        it('toggles chunk selection', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            act(() => {
                result.current.handleSelectChunk('chunk-1');
            });

            expect(hoisted.storeState.toggleChunkSelection).toHaveBeenCalledWith('chunk-1');
        });

        it('adds chunk to selection when not selected', () => {
            const {rerender, result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            act(() => {
                result.current.handleSelectChunk('chunk-1');
            });

            rerender();

            expect(result.current.selectedChunks).toContain('chunk-1');
        });

        it('removes chunk from selection when already selected', () => {
            hoisted.storeState.selectedChunks = ['chunk-1'];

            const {rerender, result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            act(() => {
                result.current.handleSelectChunk('chunk-1');
            });

            rerender();

            expect(result.current.selectedChunks).not.toContain('chunk-1');
        });

        it('supports selecting multiple chunks', () => {
            const {rerender, result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            act(() => {
                result.current.handleSelectChunk('chunk-1');
            });

            rerender();

            act(() => {
                result.current.handleSelectChunk('chunk-2');
            });

            rerender();

            expect(result.current.selectedChunks).toContain('chunk-1');
            expect(result.current.selectedChunks).toContain('chunk-2');
        });
    });

    describe('handleClearSelection', () => {
        it('clears all selected chunks', () => {
            hoisted.storeState.selectedChunks = ['chunk-1', 'chunk-2', 'chunk-3'];

            const {rerender, result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            act(() => {
                result.current.handleClearSelection();
            });

            rerender();

            expect(hoisted.storeState.clearSelection).toHaveBeenCalled();
            expect(result.current.selectedChunks).toEqual([]);
        });

        it('does nothing when selection is already empty', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            act(() => {
                result.current.handleClearSelection();
            });

            expect(hoisted.storeState.clearSelection).toHaveBeenCalled();
        });
    });

    describe('selectedChunks', () => {
        it('reflects current selection state', () => {
            hoisted.storeState.selectedChunks = ['chunk-1', 'chunk-2'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkList());

            expect(result.current.selectedChunks).toEqual(['chunk-1', 'chunk-2']);
        });
    });
});
