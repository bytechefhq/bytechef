import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseDocumentList from '../useKnowledgeBaseDocumentList';

const hoisted = vi.hoisted(() => {
    return {
        allTagsData: {
            knowledgeBaseDocumentTags: [] as Array<{id: string; name: string}>,
        },
        tagsByDocumentData: {
            knowledgeBaseDocumentTagsByDocument: [] as Array<{
                knowledgeBaseDocumentId: string;
                tags: Array<{id: string; name: string}>;
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

const createMockDocument = (id: string, chunks: Array<{content: string; id: string}> = []) => ({
    chunks: chunks.map((chunk) => ({...chunk, knowledgeBaseDocumentId: id, metadata: null})),
    createdDate: '2024-01-01',
    document: null,
    id,
    name: `Document ${id}`,
    status: 2,
});

describe('useKnowledgeBaseDocumentList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.tagsByDocumentData = {knowledgeBaseDocumentTagsByDocument: []};
        hoisted.allTagsData = {knowledgeBaseDocumentTags: []};
    });

    describe('getSortedChunksForDocument', () => {
        it('returns empty array for document with no chunks', () => {
            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            expect(result.current.getSortedChunksForDocument('doc-1')).toEqual([]);
        });

        it('returns sorted chunks by id', () => {
            const documents = [
                createMockDocument('doc-1', [
                    {content: 'Content C', id: 'chunk-c'},
                    {content: 'Content A', id: 'chunk-a'},
                    {content: 'Content B', id: 'chunk-b'},
                ]),
            ];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const sortedChunks = result.current.getSortedChunksForDocument('doc-1');

            expect(sortedChunks[0].id).toBe('chunk-a');
            expect(sortedChunks[1].id).toBe('chunk-b');
            expect(sortedChunks[2].id).toBe('chunk-c');
        });

        it('returns empty array for non-existent document', () => {
            const documents = [createMockDocument('doc-1', [{content: 'Content', id: 'chunk-1'}])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            expect(result.current.getSortedChunksForDocument('non-existent')).toEqual([]);
        });

        it('handles multiple documents correctly', () => {
            const documents = [
                createMockDocument('doc-1', [
                    {content: 'Content 1B', id: 'chunk-1b'},
                    {content: 'Content 1A', id: 'chunk-1a'},
                ]),
                createMockDocument('doc-2', [
                    {content: 'Content 2B', id: 'chunk-2b'},
                    {content: 'Content 2A', id: 'chunk-2a'},
                ]),
            ];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const doc1Chunks = result.current.getSortedChunksForDocument('doc-1');
            const doc2Chunks = result.current.getSortedChunksForDocument('doc-2');

            expect(doc1Chunks[0].id).toBe('chunk-1a');
            expect(doc1Chunks[1].id).toBe('chunk-1b');
            expect(doc2Chunks[0].id).toBe('chunk-2a');
            expect(doc2Chunks[1].id).toBe('chunk-2b');
        });

        it('filters out null chunks', () => {
            const documents = [
                {
                    ...createMockDocument('doc-1', []),
                    chunks: [
                        {content: 'Content', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null},
                        null,
                        {content: 'Content 2', id: 'chunk-2', knowledgeBaseDocumentId: 'doc-1', metadata: null},
                    ],
                },
            ];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const chunks = result.current.getSortedChunksForDocument('doc-1');

            expect(chunks).toHaveLength(2);
        });
    });

    describe('getTagsForDocument', () => {
        it('returns empty array when no tags for document', () => {
            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            expect(result.current.getTagsForDocument('doc-1')).toEqual([]);
        });

        it('returns tags for document', () => {
            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: [
                            {id: 'tag-1', name: 'Tag 1'},
                            {id: 'tag-2', name: 'Tag 2'},
                        ],
                    },
                ],
            };

            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const tags = result.current.getTagsForDocument('doc-1');

            expect(tags).toHaveLength(2);
            expect(tags[0].name).toBe('Tag 1');
            expect(tags[1].name).toBe('Tag 2');
        });

        it('returns empty array for non-existent document', () => {
            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: [{id: 'tag-1', name: 'Tag 1'}],
                    },
                ],
            };

            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            expect(result.current.getTagsForDocument('doc-2')).toEqual([]);
        });
    });

    describe('getRemainingTagsForDocument', () => {
        it('returns all tags when document has no tags', () => {
            hoisted.allTagsData = {
                knowledgeBaseDocumentTags: [
                    {id: 'tag-1', name: 'Tag 1'},
                    {id: 'tag-2', name: 'Tag 2'},
                    {id: 'tag-3', name: 'Tag 3'},
                ],
            };

            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const remainingTags = result.current.getRemainingTagsForDocument('doc-1');

            expect(remainingTags).toHaveLength(3);
        });

        it('filters out tags already assigned to document', () => {
            hoisted.allTagsData = {
                knowledgeBaseDocumentTags: [
                    {id: 'tag-1', name: 'Tag 1'},
                    {id: 'tag-2', name: 'Tag 2'},
                    {id: 'tag-3', name: 'Tag 3'},
                ],
            };

            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: [{id: 'tag-1', name: 'Tag 1'}],
                    },
                ],
            };

            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const remainingTags = result.current.getRemainingTagsForDocument('doc-1');

            expect(remainingTags).toHaveLength(2);
            expect(remainingTags.find((tag) => tag.id === 'tag-1')).toBeUndefined();
            expect(remainingTags.find((tag) => tag.id === 'tag-2')).toBeDefined();
            expect(remainingTags.find((tag) => tag.id === 'tag-3')).toBeDefined();
        });

        it('returns empty array when document has all tags', () => {
            hoisted.allTagsData = {
                knowledgeBaseDocumentTags: [
                    {id: 'tag-1', name: 'Tag 1'},
                    {id: 'tag-2', name: 'Tag 2'},
                ],
            };

            hoisted.tagsByDocumentData = {
                knowledgeBaseDocumentTagsByDocument: [
                    {
                        knowledgeBaseDocumentId: 'doc-1',
                        tags: [
                            {id: 'tag-1', name: 'Tag 1'},
                            {id: 'tag-2', name: 'Tag 2'},
                        ],
                    },
                ],
            };

            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            const remainingTags = result.current.getRemainingTagsForDocument('doc-1');

            expect(remainingTags).toHaveLength(0);
        });
    });

    describe('return values', () => {
        it('returns all expected functions', () => {
            const documents = [createMockDocument('doc-1', [])];

            const {result} = renderHook(() => useKnowledgeBaseDocumentList({documents}));

            expect(typeof result.current.getSortedChunksForDocument).toBe('function');
            expect(typeof result.current.getTagsForDocument).toBe('function');
            expect(typeof result.current.getRemainingTagsForDocument).toBe('function');
        });
    });
});
