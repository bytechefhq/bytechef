import {act, renderHook} from '@testing-library/react';
import {FormEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseSearchInterface from '../useKnowledgeBaseSearchInterface';

const hoisted = vi.hoisted(() => {
    return {
        searchResults: [
            {content: 'Result 1', id: 'result-1', metadata: {file_name: 'doc.pdf'}, score: 0.95},
            {content: 'Result 2', id: 'result-2', metadata: {file_name: 'doc.pdf'}, score: 0.85},
        ],
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useSearchKnowledgeBaseQuery: vi.fn(() => ({
        data: {searchKnowledgeBase: hoisted.searchResults},
        isLoading: false,
    })),
}));

describe('useKnowledgeBaseSearchInterface', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('has empty query', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            expect(result.current.query).toBe('');
        });

        it('has empty metadataFilters', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            expect(result.current.metadataFilters).toBe('');
        });

        it('has empty searchQuery', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            expect(result.current.searchQuery).toBe('');
        });

        it('canSearch is false', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            expect(result.current.canSearch).toBe(false);
        });
    });

    describe('setQuery', () => {
        it('updates query', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setQuery('test query');
            });

            expect(result.current.query).toBe('test query');
        });

        it('enables search when query is not empty', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setQuery('test');
            });

            expect(result.current.canSearch).toBe(true);
        });
    });

    describe('setMetadataFilters', () => {
        it('updates metadata filters', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setMetadataFilters('{"category": "docs"}');
            });

            expect(result.current.metadataFilters).toBe('{"category": "docs"}');
        });
    });

    describe('handleSearch', () => {
        const createMockEvent = (): FormEvent =>
            ({
                preventDefault: vi.fn(),
            }) as unknown as FormEvent;

        it('prevents default form submission', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));
            const mockEvent = createMockEvent();

            act(() => {
                result.current.setQuery('test');
            });

            act(() => {
                result.current.handleSearch(mockEvent);
            });

            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('sets searchQuery from query', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setQuery('search term');
            });

            act(() => {
                result.current.handleSearch(createMockEvent());
            });

            expect(result.current.searchQuery).toBe('search term');
        });

        it('does not search when query is empty', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleSearch(createMockEvent());
            });

            expect(result.current.searchQuery).toBe('');
        });

        it('does not search when query is only whitespace', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setQuery('   ');
            });

            act(() => {
                result.current.handleSearch(createMockEvent());
            });

            expect(result.current.searchQuery).toBe('');
        });
    });

    describe('handleClearSearch', () => {
        it('clears query', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setQuery('test');
            });

            act(() => {
                result.current.handleClearSearch();
            });

            expect(result.current.query).toBe('');
        });

        it('clears metadataFilters', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setMetadataFilters('{"test": true}');
            });

            act(() => {
                result.current.handleClearSearch();
            });

            expect(result.current.metadataFilters).toBe('');
        });

        it('clears searchQuery', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));
            const createMockEvent = (): FormEvent =>
                ({
                    preventDefault: vi.fn(),
                }) as unknown as FormEvent;

            act(() => {
                result.current.setQuery('test');
            });

            act(() => {
                result.current.handleSearch(createMockEvent());
            });

            act(() => {
                result.current.handleClearSearch();
            });

            expect(result.current.searchQuery).toBe('');
        });
    });

    describe('results', () => {
        it('returns search results', () => {
            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            expect(result.current.results).toEqual(hoisted.searchResults);
        });

        it('filters out null results', () => {
            hoisted.searchResults = [
                {content: 'Result 1', id: 'result-1', metadata: null as unknown as {file_name: string}, score: 0.95},
                null as unknown as (typeof hoisted.searchResults)[0],
            ];

            const {result} = renderHook(() => useKnowledgeBaseSearchInterface({knowledgeBaseId: 'kb-1'}));

            expect(result.current.results).toHaveLength(1);
        });
    });
});
