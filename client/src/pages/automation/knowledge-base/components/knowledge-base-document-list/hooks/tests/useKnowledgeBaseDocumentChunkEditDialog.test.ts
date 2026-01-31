import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentChunkEditDialog from '../useKnowledgeBaseDocumentChunkEditDialog';

const hoisted = vi.hoisted(() => {
    return {
        invalidateQueries: vi.fn(),
        storeState: {
            chunk: null as {content: string; id: string; knowledgeBaseDocumentId: string} | null,
            clearDialog: vi.fn(),
            content: '',
            setChunk: vi.fn(),
            setContent: vi.fn(),
        },
        toast: vi.fn(),
        updateMutate: vi.fn(),
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: vi.fn(() => ({
        toast: hoisted.toast,
    })),
}));

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkEditDialogStore', () => ({
    useKnowledgeBaseDocumentChunkEditDialogStore: vi.fn(() => ({
        chunk: hoisted.storeState.chunk,
        clearDialog: () => {
            hoisted.storeState.chunk = null;
            hoisted.storeState.content = '';
            hoisted.storeState.clearDialog();
        },
        content: hoisted.storeState.content,
        setChunk: (chunk: {content: string; id: string; knowledgeBaseDocumentId: string}) => {
            hoisted.storeState.chunk = chunk;
            hoisted.storeState.content = chunk.content;
            hoisted.storeState.setChunk(chunk);
        },
        setContent: (content: string) => {
            hoisted.storeState.content = content;
            hoisted.storeState.setContent(content);
        },
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateKnowledgeBaseDocumentChunkMutation: vi.fn((options: {onSuccess: () => void; onError: () => void}) => ({
        isPending: false,
        mutate: (vars: unknown) => {
            hoisted.updateMutate(vars);
            options.onSuccess();
        },
    })),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

describe('useKnowledgeBaseDocumentChunkEditDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.chunk = null;
        hoisted.storeState.content = '';
    });

    describe('initial state', () => {
        it('returns closed state when no chunk is set', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(false);
            expect(result.current.content).toBe('');
        });

        it('returns open state when chunk is set', () => {
            hoisted.storeState.chunk = {content: 'Test content', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};
            hoisted.storeState.content = 'Test content';

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(true);
            expect(result.current.content).toBe('Test content');
        });
    });

    describe('handleOpen', () => {
        it('sets the chunk to edit', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            const chunk = {content: 'Original content', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};

            act(() => {
                result.current.handleOpen(chunk);
            });

            expect(hoisted.storeState.setChunk).toHaveBeenCalledWith(chunk);
        });
    });

    describe('handleContentChange', () => {
        it('updates the content', () => {
            hoisted.storeState.chunk = {content: 'Original', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};
            hoisted.storeState.content = 'Original';

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleContentChange('Updated content');
            });

            expect(hoisted.storeState.setContent).toHaveBeenCalledWith('Updated content');
        });
    });

    describe('handleSave', () => {
        it('does nothing when no chunk is set', () => {
            hoisted.storeState.chunk = null;

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleSave();
            });

            expect(hoisted.updateMutate).not.toHaveBeenCalled();
        });

        it('calls update mutation with correct data', () => {
            hoisted.storeState.chunk = {content: 'Original', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};
            hoisted.storeState.content = 'Updated content';

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleSave();
            });

            expect(hoisted.updateMutate).toHaveBeenCalledWith({
                id: 'chunk-1',
                knowledgeBaseDocumentChunk: {
                    content: 'Updated content',
                },
            });
        });

        it('shows success toast after update', () => {
            hoisted.storeState.chunk = {content: 'Original', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};
            hoisted.storeState.content = 'Updated';

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleSave();
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Chunk updated successfully.',
            });
        });

        it('invalidates queries and clears dialog on success', () => {
            hoisted.storeState.chunk = {content: 'Original', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};
            hoisted.storeState.content = 'Updated';

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleSave();
            });

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['knowledgeBase', {id: 'kb-1'}],
            });
            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });
    });

    describe('handleClose', () => {
        it('clears the dialog', () => {
            hoisted.storeState.chunk = {content: 'Test', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'};

            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('clears dialog when open is false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });

        it('does not clear dialog when open is true', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.storeState.clearDialog).not.toHaveBeenCalled();
        });
    });

    describe('isPending', () => {
        it('returns isPending from mutation', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.isPending).toBe(false);
        });
    });
});
