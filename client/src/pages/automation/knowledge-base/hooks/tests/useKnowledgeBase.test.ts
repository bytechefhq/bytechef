import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBase from '../useKnowledgeBase';

const hoisted = vi.hoisted(() => {
    return {
        knowledgeBaseData: {
            knowledgeBase: {
                description: 'Test description',
                documents: [
                    {id: 'doc-1', name: 'Document 1'},
                    {id: 'doc-2', name: 'Document 2'},
                ],
                id: 'kb-1',
                maxChunkSize: 1024,
                minChunkSizeChars: 1,
                name: 'Test KB',
                overlap: 200,
            },
        },
        navigate: vi.fn(),
        paramId: 'kb-1',
    };
});

vi.mock('react-router-dom', () => ({
    useNavigate: vi.fn(() => hoisted.navigate),
    useParams: vi.fn(() => ({id: hoisted.paramId})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBaseQuery: vi.fn(() => ({
        data: hoisted.knowledgeBaseData,
        error: null,
        isLoading: false,
    })),
}));

describe('useKnowledgeBase', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.paramId = 'kb-1';
        hoisted.knowledgeBaseData = {
            knowledgeBase: {
                description: 'Test description',
                documents: [
                    {id: 'doc-1', name: 'Document 1'},
                    {id: 'doc-2', name: 'Document 2'},
                ],
                id: 'kb-1',
                maxChunkSize: 1024,
                minChunkSizeChars: 1,
                name: 'Test KB',
                overlap: 200,
            },
        };
    });

    describe('knowledgeBaseId', () => {
        it('returns id from params', () => {
            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.knowledgeBaseId).toBe('kb-1');
        });

        it('returns empty string when no id in params', () => {
            hoisted.paramId = undefined as unknown as string;

            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.knowledgeBaseId).toBe('');
        });
    });

    describe('knowledgeBase', () => {
        it('returns knowledge base with documents', () => {
            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.knowledgeBase).toEqual({
                ...hoisted.knowledgeBaseData.knowledgeBase,
                documents: [
                    {id: 'doc-1', name: 'Document 1'},
                    {id: 'doc-2', name: 'Document 2'},
                ],
            });
        });

        it('returns undefined when no data', () => {
            hoisted.knowledgeBaseData = null as unknown as typeof hoisted.knowledgeBaseData;

            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.knowledgeBase).toBeUndefined();
        });
    });

    describe('documents', () => {
        it('returns documents from knowledge base', () => {
            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.documents).toEqual([
                {id: 'doc-1', name: 'Document 1'},
                {id: 'doc-2', name: 'Document 2'},
            ]);
        });

        it('filters out null documents', () => {
            hoisted.knowledgeBaseData = {
                knowledgeBase: {
                    ...hoisted.knowledgeBaseData.knowledgeBase,
                    documents: [{id: 'doc-1', name: 'Document 1'}, null as unknown as {id: string; name: string}],
                },
            };

            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.documents).toEqual([{id: 'doc-1', name: 'Document 1'}]);
        });

        it('returns empty array when no documents', () => {
            hoisted.knowledgeBaseData = {
                knowledgeBase: {
                    ...hoisted.knowledgeBaseData.knowledgeBase,
                    documents: null as unknown as typeof hoisted.knowledgeBaseData.knowledgeBase.documents,
                },
            };

            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.documents).toEqual([]);
        });
    });

    describe('handleBackClick', () => {
        it('navigates to knowledge bases list', () => {
            const {result} = renderHook(() => useKnowledgeBase());

            act(() => {
                result.current.handleBackClick();
            });

            expect(hoisted.navigate).toHaveBeenCalledWith('/automation/knowledge-bases');
        });
    });

    describe('isLoading', () => {
        it('returns loading state from query', () => {
            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.isLoading).toBe(false);
        });
    });

    describe('error', () => {
        it('returns error from query', () => {
            const {result} = renderHook(() => useKnowledgeBase());

            expect(result.current.error).toBeNull();
        });
    });
});
