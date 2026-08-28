import * as graphql from '@/shared/middleware/graphql';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBases from '../useKnowledgeBases';

const hoisted = vi.hoisted(() => {
    return {
        allTagsData: {
            knowledgeBaseTags: [
                {id: '1', name: 'Tag 1'},
                {id: '2', name: 'Tag 2'},
            ],
        },
        currentWorkspaceId: 1049,
        knowledgeBasesData: {
            knowledgeBases: [
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ],
        },
        searchParams: new Map<string, string | null>([['tagId', null]]),
        tagsByKnowledgeBaseData: {
            knowledgeBaseTagsByKnowledgeBase: [
                {knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]},
                {knowledgeBaseId: 'kb-2', tags: [{id: '2', name: 'Tag 2'}]},
            ],
        },
    };
});

vi.mock('react-router-dom', () => ({
    useSearchParams: vi.fn(() => [
        {
            get: (key: string) => hoisted.searchParams.get(key) ?? null,
        },
    ]),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => number) =>
        selector({currentWorkspaceId: hoisted.currentWorkspaceId})
    ),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 0),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEmbeddedKnowledgeBasesQuery: vi.fn(() => ({
        data: {embeddedKnowledgeBases: []},
        error: null,
        isLoading: false,
    })),
    useKnowledgeBaseTagsByKnowledgeBaseQuery: vi.fn(() => ({
        data: hoisted.tagsByKnowledgeBaseData,
    })),
    useKnowledgeBaseTagsQuery: vi.fn(() => ({
        data: hoisted.allTagsData,
    })),
    useKnowledgeBasesQuery: vi.fn(() => ({
        data: hoisted.knowledgeBasesData,
        error: null,
        isLoading: false,
    })),
}));

describe('useKnowledgeBases', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.searchParams = new Map<string, string | null>([['tagId', null]]);
        hoisted.knowledgeBasesData = {
            knowledgeBases: [
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ],
        };
    });

    describe('knowledgeBases', () => {
        it('returns knowledge bases from query', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.knowledgeBases).toEqual([
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ]);
        });

        it('filters out null knowledge bases', () => {
            hoisted.knowledgeBasesData = {
                knowledgeBases: [
                    {id: 'kb-1', name: 'KB 1'},
                    null as unknown as {id: string; name: string},
                    {id: 'kb-2', name: 'KB 2'},
                ],
            };

            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.knowledgeBases).toEqual([
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ]);
        });
    });

    describe('filteredKnowledgeBases', () => {
        it('returns all knowledge bases when no tag filter', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.filteredKnowledgeBases).toEqual(result.current.knowledgeBases);
        });

        it('filters by tag when tagId is set', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '1']]);

            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.filteredKnowledgeBases).toEqual([{id: 'kb-1', name: 'KB 1'}]);
        });

        it('returns empty array when no matches for tag filter', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '999']]);

            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.filteredKnowledgeBases).toEqual([]);
        });
    });

    describe('allTags', () => {
        it('returns all tags from query', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.allTags).toEqual([
                {id: '1', name: 'Tag 1'},
                {id: '2', name: 'Tag 2'},
            ]);
        });

        it('returns empty array when no tags', () => {
            hoisted.allTagsData = {knowledgeBaseTags: null as unknown as typeof hoisted.allTagsData.knowledgeBaseTags};

            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.allTags).toEqual([]);
        });
    });

    describe('tagsByKnowledgeBaseData', () => {
        it('returns tags by knowledge base from query', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.tagsByKnowledgeBaseData).toEqual([
                {knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]},
                {knowledgeBaseId: 'kb-2', tags: [{id: '2', name: 'Tag 2'}]},
            ]);
        });
    });

    describe('tagId', () => {
        it('returns undefined when no tag filter', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.tagId).toBeUndefined();
        });

        it('returns tagId from search params', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '1']]);

            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.tagId).toBe('1');
        });
    });

    describe('isLoading', () => {
        it('returns loading state from query', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.isLoading).toBe(false);
        });
    });

    describe('error', () => {
        it('returns error from query', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'WORKSPACE', workspaceId: 1049}));

            expect(result.current.error).toBeNull();
        });
    });

    describe('scope', () => {
        // Tags are workspace-scoped. A disabled TanStack query still serves what is cached under its key, so the
        // embedded scope has to drop them on the read rather than relying on `enabled`.
        it('returns no tags under an embedded scope', () => {
            const {result} = renderHook(() => useKnowledgeBases({type: 'EMBEDDED'}));

            expect(result.current.allTags).toEqual([]);
        });

        it('reads the embedded query under an embedded scope', () => {
            renderHook(() => useKnowledgeBases({ownerId: 42, type: 'EMBEDDED'}));

            expect(graphql.useEmbeddedKnowledgeBasesQuery).toHaveBeenCalledWith(
                expect.objectContaining({ownerId: '42'}),
                expect.objectContaining({enabled: true})
            );
        });
    });
});
