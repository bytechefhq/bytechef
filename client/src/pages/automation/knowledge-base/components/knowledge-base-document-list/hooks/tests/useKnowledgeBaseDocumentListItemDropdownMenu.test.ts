import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentListItemDropdownMenu from '../useKnowledgeBaseDocumentListItemDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        setDocumentId: vi.fn(),
    };
});

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentDeleteDialogStore', () => ({
    useKnowledgeBaseDocumentDeleteDialogStore: vi.fn(() => ({
        setDocumentId: hoisted.setDocumentId,
    })),
}));

describe('useKnowledgeBaseDocumentListItemDropdownMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('handleDelete', () => {
        it('calls setDocumentId with the document id', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDropdownMenu({documentId: 'doc-1'}));

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.setDocumentId).toHaveBeenCalledWith('doc-1');
        });

        it('opens delete dialog for the correct document', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDropdownMenu({documentId: 'doc-99'}));

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.setDocumentId).toHaveBeenCalledWith('doc-99');
        });

        it('works with different document ids', () => {
            const {result: result1} = renderHook(() =>
                useKnowledgeBaseDocumentListItemDropdownMenu({documentId: 'doc-1'})
            );

            const {result: result2} = renderHook(() =>
                useKnowledgeBaseDocumentListItemDropdownMenu({documentId: 'doc-2'})
            );

            act(() => {
                result1.current.handleDelete();
            });

            expect(hoisted.setDocumentId).toHaveBeenCalledWith('doc-1');

            act(() => {
                result2.current.handleDelete();
            });

            expect(hoisted.setDocumentId).toHaveBeenCalledWith('doc-2');
        });
    });

    describe('return values', () => {
        it('returns handleDelete function', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentListItemDropdownMenu({documentId: 'doc-1'}));

            expect(typeof result.current.handleDelete).toBe('function');
        });
    });
});
