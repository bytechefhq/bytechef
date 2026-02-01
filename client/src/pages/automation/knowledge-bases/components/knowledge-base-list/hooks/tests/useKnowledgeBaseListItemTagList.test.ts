import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseListItemTagList from '../useKnowledgeBaseListItemTagList';

const hoisted = vi.hoisted(() => {
    return {
        cancelQueries: vi.fn(),
        getQueryData: vi.fn(),
        invalidateQueries: vi.fn(),
        setQueryData: vi.fn(),
        updateTagsMutation: {mutate: vi.fn()},
    };
});

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        cancelQueries: hoisted.cancelQueries,
        getQueryData: hoisted.getQueryData,
        invalidateQueries: hoisted.invalidateQueries,
        setQueryData: hoisted.setQueryData,
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateKnowledgeBaseTagsMutation: vi.fn(() => hoisted.updateTagsMutation),
}));

const mockTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
];

const mockRemainingTags = [
    {id: '3', name: 'Tag 3'},
    {id: '4', name: 'Tag 4'},
];

describe('useKnowledgeBaseListItemTagList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('convertedTags', () => {
        it('converts tag ids to numbers', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseListItemTagList({
                    knowledgeBaseId: 'kb-1',
                    remainingTags: mockRemainingTags,
                    tags: mockTags,
                })
            );

            expect(result.current.convertedTags).toEqual([
                {id: 1, name: 'Tag 1'},
                {id: 2, name: 'Tag 2'},
            ]);
        });

        it('handles empty tags array', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseListItemTagList({
                    knowledgeBaseId: 'kb-1',
                    remainingTags: mockRemainingTags,
                    tags: [],
                })
            );

            expect(result.current.convertedTags).toEqual([]);
        });
    });

    describe('convertedRemainingTags', () => {
        it('converts remaining tag ids to numbers', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseListItemTagList({
                    knowledgeBaseId: 'kb-1',
                    remainingTags: mockRemainingTags,
                    tags: mockTags,
                })
            );

            expect(result.current.convertedRemainingTags).toEqual([
                {id: 3, name: 'Tag 3'},
                {id: 4, name: 'Tag 4'},
            ]);
        });

        it('handles undefined remainingTags', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseListItemTagList({
                    knowledgeBaseId: 'kb-1',
                    remainingTags: undefined,
                    tags: mockTags,
                })
            );

            expect(result.current.convertedRemainingTags).toBeUndefined();
        });
    });

    describe('updateTagsMutation', () => {
        it('returns update tags mutation', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseListItemTagList({
                    knowledgeBaseId: 'kb-1',
                    remainingTags: mockRemainingTags,
                    tags: mockTags,
                })
            );

            expect(result.current.updateTagsMutation).toBe(hoisted.updateTagsMutation);
        });
    });
});
