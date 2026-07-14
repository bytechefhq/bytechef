import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentList from '../useKnowledgeBaseDocumentList';

const hoisted = vi.hoisted(() => {
    return {
        allTagsData: {
            knowledgeBaseDocumentTags: [] as string[],
        },
        tagsByDocumentData: {
            knowledgeBaseDocumentTagsByDocument: [] as Array<{
                knowledgeBaseDocumentId: string;
                tags: string[];
            }>,
        },
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBaseDocumentTagsByDocumentQuery: vi.fn(() => ({
        data: hoisted.tagsByDocumentData,
    })),
    useKnowledgeBaseDocumentTagsQuery: vi.fn(() => ({
        data: hoisted.allTagsData,
    })),
}));

describe('useKnowledgeBaseDocumentList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.tagsByDocumentData = {knowledgeBaseDocumentTagsByDocument: []};
        hoisted.allTagsData = {knowledgeBaseDocumentTags: []};
    });

    describe('getTagsForDocument', () => {
        it('returns empty array when no tags for document', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            expect(result.current.getTagsForDocument('doc-1')).toEqual([]);
        });

        it('returns tags for document', () => {
            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: ['Tag 1', 'Tag 2'],
                    },
                ],
            };

            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            const tags = result.current.getTagsForDocument('doc-1');

            expect(tags).toHaveLength(2);
            expect(tags[0]).toBe('Tag 1');
            expect(tags[1]).toBe('Tag 2');
        });

        it('returns empty array for non-existent document', () => {
            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: ['Tag 1'],
                    },
                ],
            };

            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            expect(result.current.getTagsForDocument('doc-2')).toEqual([]);
        });
    });

    describe('getRemainingTagsForDocument', () => {
        it('returns all tags when document has no tags', () => {
            hoisted.allTagsData = {
                knowledgeBaseDocumentTags: ['Tag 1', 'Tag 2', 'Tag 3'],
            };

            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            const remainingTags = result.current.getRemainingTagsForDocument('doc-1');

            expect(remainingTags).toHaveLength(3);
        });

        it('filters out tags already assigned to document', () => {
            hoisted.allTagsData = {
                knowledgeBaseDocumentTags: ['Tag 1', 'Tag 2', 'Tag 3'],
            };

            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: ['Tag 1'],
                    },
                ],
            };

            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            const remainingTags = result.current.getRemainingTagsForDocument('doc-1');

            expect(remainingTags).toHaveLength(2);
            expect(remainingTags.find((tagName) => tagName === 'Tag 1')).toBeUndefined();
            expect(remainingTags.find((tagName) => tagName === 'Tag 2')).toBeDefined();
            expect(remainingTags.find((tagName) => tagName === 'Tag 3')).toBeDefined();
        });

        it('returns empty array when document has all tags', () => {
            hoisted.allTagsData = {
                knowledgeBaseDocumentTags: ['Tag 1', 'Tag 2'],
            };

            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: ['Tag 1', 'Tag 2'],
                    },
                ],
            };

            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            const remainingTags = result.current.getRemainingTagsForDocument('doc-1');

            expect(remainingTags).toHaveLength(0);
        });
    });

    describe('return values', () => {
        it('returns all expected functions', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentList());

            expect(typeof result.current.getTagsForDocument).toBe('function');
            expect(typeof result.current.getRemainingTagsForDocument).toBe('function');
        });
    });
});
