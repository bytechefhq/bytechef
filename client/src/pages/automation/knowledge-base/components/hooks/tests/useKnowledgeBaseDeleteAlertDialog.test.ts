import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDeleteAlertDialog from '../useKnowledgeBaseDeleteAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        deleteMutate: vi.fn(),
        invalidateQueries: vi.fn(),
        navigate: vi.fn(),
        toast: vi.fn(),
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: vi.fn(() => ({
        toast: hoisted.toast,
    })),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: vi.fn(() => hoisted.navigate),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteKnowledgeBaseMutation: vi.fn((options: {onError: () => void; onSuccess: () => void}) => ({
        isPending: false,
        mutate: (vars: unknown) => {
            hoisted.deleteMutate(vars);
            options.onSuccess();
        },
    })),
}));

describe('useKnowledgeBaseDeleteAlertDialog', () => {
    const mockOnClose = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('handleDeleteClick', () => {
        it('calls delete mutation with knowledge base id', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDeleteAlertDialog({
                    knowledgeBaseId: 'kb-1',
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleDeleteClick();
            });

            expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'kb-1'});
        });

        it('calls onClose after delete', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDeleteAlertDialog({
                    knowledgeBaseId: 'kb-1',
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleDeleteClick();
            });

            expect(mockOnClose).toHaveBeenCalled();
        });
    });

    describe('handleCancelClick', () => {
        it('calls onClose', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDeleteAlertDialog({
                    knowledgeBaseId: 'kb-1',
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleCancelClick();
            });

            expect(mockOnClose).toHaveBeenCalled();
        });
    });

    describe('on success', () => {
        it('invalidates queries after delete', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDeleteAlertDialog({
                    knowledgeBaseId: 'kb-1',
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleDeleteClick();
            });

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['knowledgeBases']});
        });

        it('shows success toast after delete', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDeleteAlertDialog({
                    knowledgeBaseId: 'kb-1',
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleDeleteClick();
            });

            expect(hoisted.toast).toHaveBeenCalledWith({description: 'Knowledge base deleted successfully.'});
        });

        it('navigates to knowledge bases list after delete', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDeleteAlertDialog({
                    knowledgeBaseId: 'kb-1',
                    onClose: mockOnClose,
                })
            );

            act(() => {
                result.current.handleDeleteClick();
            });

            expect(hoisted.navigate).toHaveBeenCalledWith('/automation/knowledge-bases');
        });
    });
});
