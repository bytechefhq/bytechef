import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseLeftSidebarNav from '../useKnowledgeBaseLeftSidebarNav';

const hoisted = vi.hoisted(() => {
    return {
        currentWorkspaceId: 1049,
        knowledgeBasesData: {
            knowledgeBases: [
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ],
        },
        paramId: 'kb-1',
    };
});

vi.mock('react-router-dom', () => ({
    useParams: vi.fn(() => ({id: hoisted.paramId})),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => number) =>
        selector({currentWorkspaceId: hoisted.currentWorkspaceId})
    ),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBasesQuery: vi.fn(() => ({
        data: hoisted.knowledgeBasesData,
        isLoading: false,
    })),
}));

describe('useKnowledgeBaseLeftSidebarNav', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.paramId = 'kb-1';
        hoisted.knowledgeBasesData = {
            knowledgeBases: [
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ],
        };
    });

    describe('currentKnowledgeBaseId', () => {
        it('returns current id from params', () => {
            const {result} = renderHook(() => useKnowledgeBaseLeftSidebarNav());

            expect(result.current.currentKnowledgeBaseId).toBe('kb-1');
        });
    });

    describe('knowledgeBases', () => {
        it('returns knowledge bases from query', () => {
            const {result} = renderHook(() => useKnowledgeBaseLeftSidebarNav());

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

            const {result} = renderHook(() => useKnowledgeBaseLeftSidebarNav());

            expect(result.current.knowledgeBases).toEqual([
                {id: 'kb-1', name: 'KB 1'},
                {id: 'kb-2', name: 'KB 2'},
            ]);
        });

        it('returns empty array when no data', () => {
            hoisted.knowledgeBasesData = null as unknown as typeof hoisted.knowledgeBasesData;

            const {result} = renderHook(() => useKnowledgeBaseLeftSidebarNav());

            expect(result.current.knowledgeBases).toEqual([]);
        });
    });

    describe('isLoading', () => {
        it('returns loading state from query', () => {
            const {result} = renderHook(() => useKnowledgeBaseLeftSidebarNav());

            expect(result.current.isLoading).toBe(false);
        });
    });
});
