import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBasesFilterTitle from '../useKnowledgeBasesFilterTitle';

const hoisted = vi.hoisted(() => {
    return {
        searchParams: new Map<string, string | null>([['tagId', null]]),
    };
});

vi.mock('react-router-dom', () => ({
    useSearchParams: vi.fn(() => [
        {
            get: (key: string) => hoisted.searchParams.get(key) ?? null,
        },
    ]),
}));

const mockAllTags = [
    {id: '1', name: 'Global Tag 1'},
    {id: '2', name: 'Global Tag 2'},
];

const mockTagsByKnowledgeBase = [
    {knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]},
    {knowledgeBaseId: 'kb-2', tags: [{id: '3', name: 'Tag 3'}]},
];

describe('useKnowledgeBasesFilterTitle', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.searchParams = new Map<string, string | null>([['tagId', null]]);
    });

    describe('tagId', () => {
        it('returns null when no tag filter', () => {
            const {result} = renderHook(() =>
                useKnowledgeBasesFilterTitle({
                    allTags: mockAllTags,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.tagId).toBeNull();
        });

        it('returns tagId from search params', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '1']]);

            const {result} = renderHook(() =>
                useKnowledgeBasesFilterTitle({
                    allTags: mockAllTags,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.tagId).toBe('1');
        });
    });

    describe('pageTitle', () => {
        it('returns undefined when no tag filter', () => {
            const {result} = renderHook(() =>
                useKnowledgeBasesFilterTitle({
                    allTags: mockAllTags,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.pageTitle).toBeUndefined();
        });

        it('returns tag name from global tags', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '1']]);

            const {result} = renderHook(() =>
                useKnowledgeBasesFilterTitle({
                    allTags: mockAllTags,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.pageTitle).toBe('Global Tag 1');
        });

        it('falls back to tags by knowledge base when not in global tags', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '3']]);

            const {result} = renderHook(() =>
                useKnowledgeBasesFilterTitle({
                    allTags: mockAllTags,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.pageTitle).toBe('Tag 3');
        });

        it('returns undefined when tag not found anywhere', () => {
            hoisted.searchParams = new Map<string, string | null>([['tagId', '999']]);

            const {result} = renderHook(() =>
                useKnowledgeBasesFilterTitle({
                    allTags: mockAllTags,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.pageTitle).toBeUndefined();
        });
    });
});
