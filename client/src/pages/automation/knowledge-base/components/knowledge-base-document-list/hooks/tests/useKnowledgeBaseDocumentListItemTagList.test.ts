import {act, renderHook, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentListItemTagList from '../useKnowledgeBaseDocumentListItemTagList';

const hoisted = vi.hoisted(() => {
    return {
        cancelQueries: vi.fn(),
        getQueryData: vi.fn(),
        invalidateQueries: vi.fn(),
        mutate: vi.fn(),
        setQueryData: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateKnowledgeBaseDocumentTagsMutation: vi.fn(
        (options: {
            onError: (error: unknown, variables: unknown, context: unknown) => void;
            onMutate: (variables: unknown) => Promise<unknown>;
            onSettled: () => void;
        }) => ({
            mutate: (variables: unknown) => {
                hoisted.mutate(variables);
                options.onMutate(variables);
                options.onSettled();
            },
        })
    ),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        cancelQueries: hoisted.cancelQueries,
        getQueryData: hoisted.getQueryData,
        invalidateQueries: hoisted.invalidateQueries,
        setQueryData: hoisted.setQueryData,
    })),
}));

const mockTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
];

const mockRemainingTags = [
    {id: '3', name: 'Tag 3'},
    {id: '4', name: 'Tag 4'},
];

describe('useKnowledgeBaseDocumentListItemTagList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.getQueryData.mockReturnValue({
            knowledgeBaseDocumentTagsByDocument: [],
        });
    });

    describe('convertedTags', () => {
        it('converts tag ids to numbers', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
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
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    tags: [],
                })
            );

            expect(result.current.convertedTags).toEqual([]);
        });

        it('preserves tag names during conversion', () => {
            const tagsWithLongNames = [
                {id: '100', name: 'Very Long Tag Name'},
                {id: '200', name: 'Another Long Name'},
            ];

            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    tags: tagsWithLongNames,
                })
            );

            expect(result.current.convertedTags[0].name).toBe('Very Long Tag Name');
            expect(result.current.convertedTags[1].name).toBe('Another Long Name');
        });
    });

    describe('convertedRemainingTags', () => {
        it('converts remaining tag ids to numbers', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    remainingTags: mockRemainingTags,
                    tags: mockTags,
                })
            );

            expect(result.current.convertedRemainingTags).toEqual([
                {id: 3, name: 'Tag 3'},
                {id: 4, name: 'Tag 4'},
            ]);
        });

        it('returns undefined when remainingTags is not provided', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    tags: mockTags,
                })
            );

            expect(result.current.convertedRemainingTags).toBeUndefined();
        });

        it('handles empty remainingTags array', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    remainingTags: [],
                    tags: mockTags,
                })
            );

            expect(result.current.convertedRemainingTags).toEqual([]);
        });
    });

    describe('updateTagsMutation', () => {
        it('returns mutation object', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    tags: mockTags,
                })
            );

            expect(result.current.updateTagsMutation).toBeDefined();
            expect(typeof result.current.updateTagsMutation.mutate).toBe('function');
        });

        it('calls mutate when mutation is triggered', async () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    tags: mockTags,
                })
            );

            act(() => {
                result.current.updateTagsMutation.mutate({
                    input: {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: [{id: '1', name: 'Tag 1'}],
                    },
                });
            });

            await waitFor(() => {
                expect(hoisted.mutate).toHaveBeenCalled();
            });
        });

        it('invalidates queries on settled', async () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    tags: mockTags,
                })
            );

            act(() => {
                result.current.updateTagsMutation.mutate({
                    input: {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: [{id: '1', name: 'Tag 1'}],
                    },
                });
            });

            await waitFor(() => {
                expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                    queryKey: ['knowledgeBaseDocumentTags'],
                });
                expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                    queryKey: ['knowledgeBaseDocumentTagsByDocument'],
                });
                expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
                    queryKey: ['knowledgeBases'],
                });
            });
        });
    });

    describe('memoization', () => {
        it('memoizes convertedTags when tags do not change', () => {
            const {rerender, result} = renderHook(
                ({tags}) =>
                    useKnowledgeBaseDocumentListItemTagList({
                        knowledgeBaseDocumentId: 'doc-1',
                        tags,
                    }),
                {initialProps: {tags: mockTags}}
            );

            const firstConvertedTags = result.current.convertedTags;

            rerender({tags: mockTags});

            expect(result.current.convertedTags).toBe(firstConvertedTags);
        });

        it('recalculates convertedTags when tags change', () => {
            const {rerender, result} = renderHook(
                ({tags}) =>
                    useKnowledgeBaseDocumentListItemTagList({
                        knowledgeBaseDocumentId: 'doc-1',
                        tags,
                    }),
                {initialProps: {tags: mockTags}}
            );

            const firstConvertedTags = result.current.convertedTags;

            const newTags = [{id: '5', name: 'New Tag'}];

            rerender({tags: newTags});

            expect(result.current.convertedTags).not.toBe(firstConvertedTags);
            expect(result.current.convertedTags).toEqual([{id: 5, name: 'New Tag'}]);
        });
    });

    describe('return values', () => {
        it('returns all expected properties', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseDocumentListItemTagList({
                    knowledgeBaseDocumentId: 'doc-1',
                    remainingTags: mockRemainingTags,
                    tags: mockTags,
                })
            );

            expect(result.current).toHaveProperty('convertedTags');
            expect(result.current).toHaveProperty('convertedRemainingTags');
            expect(result.current).toHaveProperty('updateTagsMutation');
        });
    });
});
