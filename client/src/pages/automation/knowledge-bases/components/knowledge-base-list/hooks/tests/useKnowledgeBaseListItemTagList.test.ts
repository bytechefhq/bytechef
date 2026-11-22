import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useUpdateKnowledgeBaseTagsMutation} from '@/shared/middleware/graphql';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseListItemTagList from '../useKnowledgeBaseListItemTagList';

interface MutationCallbacksI {
    onError: (error: Error, variables: unknown, context: {previous?: unknown}) => void;
    onMutate: (variables: {
        input: {knowledgeBaseId: string; tags: {id?: string; name: string}[]};
    }) => Promise<{previous?: unknown}>;
}

const CURRENT_WORKSPACE_KEY = ['knowledgeBaseTagsByKnowledgeBase', {environmentId: '2', workspaceId: '7'}];

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

        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
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

    describe('optimistic tag update', () => {
        function renderMutationCallbacks(): MutationCallbacksI {
            renderHook(() =>
                useKnowledgeBaseListItemTagList({
                    knowledgeBaseId: 'kb-1',
                    remainingTags: mockRemainingTags,
                    tags: mockTags,
                })
            );

            return vi.mocked(useUpdateKnowledgeBaseTagsMutation).mock.calls[0][0] as unknown as MutationCallbacksI;
        }

        it('replaces the knowledge base tags in the current workspace cache entry', async () => {
            const previous = {
                knowledgeBaseTagsByKnowledgeBase: [
                    {knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]},
                    {knowledgeBaseId: 'kb-2', tags: [{id: '2', name: 'Tag 2'}]},
                ],
            };

            hoisted.getQueryData.mockReturnValue(previous);

            const mutationCallbacks = renderMutationCallbacks();

            const context = await mutationCallbacks.onMutate({
                input: {knowledgeBaseId: 'kb-1', tags: [{id: '3', name: 'Tag 3'}]},
            });

            expect(hoisted.getQueryData).toHaveBeenCalledWith(CURRENT_WORKSPACE_KEY);
            expect(hoisted.setQueryData).toHaveBeenCalledWith(CURRENT_WORKSPACE_KEY, {
                knowledgeBaseTagsByKnowledgeBase: [
                    {knowledgeBaseId: 'kb-1', tags: [{id: '3', name: 'Tag 3'}]},
                    {knowledgeBaseId: 'kb-2', tags: [{id: '2', name: 'Tag 2'}]},
                ],
            });
            expect(context.previous).toBe(previous);
        });

        it('adds an entry for a knowledge base that had no tags', async () => {
            hoisted.getQueryData.mockReturnValue({knowledgeBaseTagsByKnowledgeBase: []});

            const mutationCallbacks = renderMutationCallbacks();

            await mutationCallbacks.onMutate({input: {knowledgeBaseId: 'kb-1', tags: [{id: '3', name: 'Tag 3'}]}});

            expect(hoisted.setQueryData).toHaveBeenCalledWith(CURRENT_WORKSPACE_KEY, {
                knowledgeBaseTagsByKnowledgeBase: [{knowledgeBaseId: 'kb-1', tags: [{id: '3', name: 'Tag 3'}]}],
            });
        });

        it('restores the current workspace cache entry when the update fails', () => {
            const previous = {knowledgeBaseTagsByKnowledgeBase: []};

            const mutationCallbacks = renderMutationCallbacks();

            mutationCallbacks.onError(new Error('failed'), undefined, {previous});

            expect(hoisted.setQueryData).toHaveBeenCalledWith(CURRENT_WORKSPACE_KEY, previous);
        });
    });
});
