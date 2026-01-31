import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentListItemDeleteDialog from '../useKnowledgeBaseDocumentListItemDeleteDialog';

const hoisted = vi.hoisted(() => {
    return {
        deleteMutate: vi.fn(),
        invalidateQueries: vi.fn(),
        storeState: {
            clearDialog: vi.fn(),
            documentId: null as string | null,
            setDocumentId: vi.fn(),
        },
        toast: vi.fn(),
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: vi.fn(() => ({
        toast: hoisted.toast,
    })),
}));

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentDeleteDialogStore', () => ({
    useKnowledgeBaseDocumentDeleteDialogStore: vi.fn(() => ({
        clearDialog: () => {
            hoisted.storeState.documentId = null;
            hoisted.storeState.clearDialog();
        },
        documentId: hoisted.storeState.documentId,
        setDocumentId: (documentId: string) => {
            hoisted.storeState.documentId = documentId;
            hoisted.storeState.setDocumentId(documentId);
        },
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteKnowledgeBaseDocumentMutation: vi.fn((options: {onSuccess: () => void; onError: () => void}) => ({
        isPending: false,
        mutate: (vars: unknown) => {
            hoisted.deleteMutate(vars);
            options.onSuccess();
        },
    })),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

describe('useKnowledgeBaseDocumentListItemDeleteDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.documentId = null;
    });

    describe('initial state', () => {
        it('returns closed state when no document to delete', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(false);
        });

        it('returns open state when document is set', () => {
            hoisted.storeState.documentId = 'doc-1';

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(true);
        });
    });

    describe('handleOpen', () => {
        it('sets the document id to delete', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpen('doc-1');
            });

            expect(hoisted.storeState.setDocumentId).toHaveBeenCalledWith('doc-1');
        });
    });

    describe('handleConfirm', () => {
        it('does nothing when no document id is set', () => {
            hoisted.storeState.documentId = null;

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleConfirm();
            });

            expect(hoisted.deleteMutate).not.toHaveBeenCalled();
        });

        it('calls delete mutation with correct document id', () => {
            hoisted.storeState.documentId = 'doc-1';

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleConfirm();
            });

            expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'doc-1'});
        });

        it('shows success toast after deletion', () => {
            hoisted.storeState.documentId = 'doc-1';

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleConfirm();
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Document deleted successfully.',
            });
        });

        it('invalidates queries after deletion', () => {
            hoisted.storeState.documentId = 'doc-1';

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleConfirm();
            });

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['knowledgeBase', {id: 'kb-1'}],
            });
            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['knowledgeBases'],
            });
        });

        it('clears dialog after successful deletion', () => {
            hoisted.storeState.documentId = 'doc-1';

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleConfirm();
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });
    });

    describe('handleClose', () => {
        it('clears the dialog', () => {
            hoisted.storeState.documentId = 'doc-1';

            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleClose();
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });
    });

    describe('handleOpenChange', () => {
        it('clears dialog when open is false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(hoisted.storeState.clearDialog).toHaveBeenCalled();
        });

        it('does not clear dialog when open is true', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(hoisted.storeState.clearDialog).not.toHaveBeenCalled();
        });
    });

    describe('isPending', () => {
        it('returns isPending from mutation', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.isPending).toBe(false);
        });
    });

    describe('return values', () => {
        it('returns all expected properties', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId: 'kb-1'}));

            expect(typeof result.current.handleClose).toBe('function');
            expect(typeof result.current.handleConfirm).toBe('function');
            expect(typeof result.current.handleOpen).toBe('function');
            expect(typeof result.current.handleOpenChange).toBe('function');
            expect(typeof result.current.isPending).toBe('boolean');
            expect(typeof result.current.open).toBe('boolean');
        });
    });
});
