import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentChunkDeleteDialog from '../useKnowledgeBaseDocumentChunkDeleteDialog';

const hoisted = vi.hoisted(() => {
    return {
        deleteMutate: vi.fn(),
        invalidateQueries: vi.fn(),
        selectionStoreState: {
            clearSelection: vi.fn(),
            selectedChunks: [] as string[],
        },
        storeState: {
            chunkIdsToDelete: [] as string[],
            clearDialog: vi.fn(),
            setChunkIdsToDelete: vi.fn(),
        },
        toast: vi.fn(),
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: vi.fn(() => ({
        toast: hoisted.toast,
    })),
}));

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkDeleteDialogStore', () => ({
    useKnowledgeBaseDocumentChunkDeleteDialogStore: vi.fn(() => ({
        chunkIdsToDelete: hoisted.storeState.chunkIdsToDelete,
        clearDialog: () => {
            hoisted.storeState.chunkIdsToDelete = [];
            hoisted.storeState.clearDialog();
        },
        setChunkIdsToDelete: (chunkIds: string[]) => {
            hoisted.storeState.chunkIdsToDelete = chunkIds;
            hoisted.storeState.setChunkIdsToDelete(chunkIds);
        },
    })),
}));

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore', () => ({
    useKnowledgeBaseDocumentChunkSelectionStore: vi.fn(() => ({
        clearSelection: hoisted.selectionStoreState.clearSelection,
        selectedChunks: hoisted.selectionStoreState.selectedChunks,
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteKnowledgeBaseDocumentChunkMutation: vi.fn((options: {onSuccess: () => void}) => ({
        isPending: false,
        mutate: (vars: unknown, mutateOptions?: {onSuccess?: () => void; onError?: (error: Error) => void}) => {
            hoisted.deleteMutate(vars);
            options.onSuccess();

            if (mutateOptions?.onSuccess) {
                mutateOptions.onSuccess();
            }
        },
    })),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

describe('useKnowledgeBaseDocumentChunkDeleteDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.chunkIdsToDelete = [];
        hoisted.selectionStoreState.selectedChunks = [];
    });

    describe('initial state', () => {
        it('returns closed state when no chunks to delete', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(false);
            expect(result.current.chunkCount).toBe(0);
        });

        it('returns open state when chunks are set to delete', () => {
            hoisted.storeState.chunkIdsToDelete = ['chunk-1', 'chunk-2'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(true);
            expect(result.current.chunkCount).toBe(2);
        });
    });

    describe('handleDeleteChunk', () => {
        it('sets single chunk id to delete', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleDeleteChunk('chunk-1');
            });

            expect(hoisted.storeState.setChunkIdsToDelete).toHaveBeenCalledWith(['chunk-1']);
        });
    });

    describe('handleDeleteSelectedChunks', () => {
        it('sets selected chunks to delete', () => {
            hoisted.selectionStoreState.selectedChunks = ['chunk-1', 'chunk-2', 'chunk-3'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleDeleteSelectedChunks();
            });

            expect(hoisted.storeState.setChunkIdsToDelete).toHaveBeenCalledWith(['chunk-1', 'chunk-2', 'chunk-3']);
        });

        it('does not set chunks when no selection', () => {
            hoisted.selectionStoreState.selectedChunks = [];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleDeleteSelectedChunks();
            });

            expect(hoisted.storeState.setChunkIdsToDelete).not.toHaveBeenCalled();
        });
    });

    describe('handleConfirm', () => {
        it('does nothing when no chunks to delete', async () => {
            hoisted.storeState.chunkIdsToDelete = [];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            await act(async () => {
                await result.current.handleConfirm();
            });

            expect(hoisted.deleteMutate).not.toHaveBeenCalled();
        });

        it('calls delete mutation for each chunk', async () => {
            hoisted.storeState.chunkIdsToDelete = ['chunk-1', 'chunk-2'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            await act(async () => {
                await result.current.handleConfirm();
            });

            expect(hoisted.deleteMutate).toHaveBeenCalledTimes(2);
            expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'chunk-1'});
            expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'chunk-2'});
        });

        it('shows success toast after deletion', async () => {
            hoisted.storeState.chunkIdsToDelete = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            await act(async () => {
                await result.current.handleConfirm();
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: '1 chunk deleted successfully.',
            });
        });

        it('clears dialog after confirmation', async () => {
            hoisted.storeState.chunkIdsToDelete = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            await act(async () => {
                await result.current.handleConfirm();
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });
    });

    describe('handleClose', () => {
        it('clears the dialog', () => {
            hoisted.storeState.chunkIdsToDelete = ['chunk-1'];

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('clears dialog when open is false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });

        it('does not clear dialog when open is true', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.storeState.clearDialog).not.toHaveBeenCalled();
        });
    });
});
