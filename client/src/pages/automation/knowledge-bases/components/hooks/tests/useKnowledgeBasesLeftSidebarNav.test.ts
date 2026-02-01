import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBasesLeftSidebarNav from '../useKnowledgeBasesLeftSidebarNav';

const hoisted = vi.hoisted(() => {
    return {
        searchParams: new Map<string, string | null>([['tagId', null]]),
        tagsData: {
            knowledgeBaseTags: [
                {id: '1', name: 'Tag 1'},
                {id: '2', name: 'Tag 2'},
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

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBaseTagsQuery: vi.fn(() => ({
        data: hoisted.tagsData,
        isLoading: false,
    })),
}));

describe('useKnowledgeBasesLeftSidebarNav', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.searchParams = new Map<string, string | null>([['tagId', null]]);
        hoisted.tagsData = {
            knowledgeBaseTags: [
                {id: '1', name: 'Tag 1'},
                {id: '2', name: 'Tag 2'},
            ],
        };
    });

    describe('tags', () => {
        it('returns tags from query', () => {
            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.tags).toEqual([
                {id: '1', name: 'Tag 1'},
                {id: '2', name: 'Tag 2'},
            ]);
        });

        it('returns empty array when no tags', () => {
            hoisted.tagsData = {knowledgeBaseTags: null as unknown as typeof hoisted.tagsData.knowledgeBaseTags};

            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.tags).toEqual([]);
        });
    });

    describe('tagId', () => {
        it('returns null when no tag filter', () => {
            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.tagId).toBeNull();
        });

        it('returns tagId from search params', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '1']]);

            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.tagId).toBe('1');
        });
    });

    describe('hasData', () => {
        it('returns true when data is available', () => {
            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.hasData).toBe(true);
        });

        it('returns false when no data', () => {
            hoisted.tagsData = null as unknown as typeof hoisted.tagsData;

            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.hasData).toBe(false);
        });
    });

    describe('isLoading', () => {
        it('returns loading state from query', () => {
            const {result} = renderHook(() => useKnowledgeBasesLeftSidebarNav());

            expect(result.current.isLoading).toBe(false);
        });
    });
});
