import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentChunkListItemDropdownMenu from '../useKnowledgeBaseDocumentChunkListItemDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        setChunk: vi.fn(),
        setChunkIdsToDelete: vi.fn(),
    };
});

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkEditDialogStore', () => ({
    useKnowledgeBaseDocumentChunkEditDialogStore: vi.fn(() => ({
        setChunk: hoisted.setChunk,
    })),
}));

vi.mock('@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkDeleteDialogStore', () => ({
    useKnowledgeBaseDocumentChunkDeleteDialogStore: vi.fn(() => ({
        setChunkIdsToDelete: hoisted.setChunkIdsToDelete,
    })),
}));

const mockChunk = {
    content: 'Test chunk content',
    id: 'chunk-1',
    knowledgeBaseDocumentId: 'doc-1',
    metadata: {page: 1},
};

describe('useKnowledgeBaseDocumentChunkListItemDropdownMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('handleEdit', () => {
        it('calls setChunk with the chunk', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk: mockChunk}));

            act(() => {
                result.current.handleEdit();
            });

            expect(hoisted.setChunk).toHaveBeenCalledWith(mockChunk);
        });

        it('opens edit dialog for the correct chunk', () => {
            const differentChunk = {
                content: 'Different content',
                id: 'chunk-2',
                knowledgeBaseDocumentId: 'doc-1',
                metadata: {page: 2},
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk: differentChunk})
            );

            act(() => {
                result.current.handleEdit();
            });

            expect(hoisted.setChunk).toHaveBeenCalledWith(differentChunk);
        });
    });

    describe('handleDelete', () => {
        it('calls setChunkIdsToDelete with the chunk id', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk: mockChunk}));

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.setChunkIdsToDelete).toHaveBeenCalledWith(['chunk-1']);
        });

        it('opens delete dialog for the correct chunk', () => {
            const differentChunk = {
                content: 'Content',
                id: 'chunk-99',
                knowledgeBaseDocumentId: 'doc-1',
                metadata: {},
            };

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk: differentChunk})
            );

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.setChunkIdsToDelete).toHaveBeenCalledWith(['chunk-99']);
        });
    });

    describe('return values', () => {
        it('returns handleEdit and handleDelete functions', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkListItemDropdownMenu({chunk: mockChunk}));

            expect(typeof result.current.handleEdit).toBe('function');
            expect(typeof result.current.handleDelete).toBe('function');
        });
    });
});
