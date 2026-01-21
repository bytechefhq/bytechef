import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTablesLeftSidebarNav from '../useDataTablesLeftSidebarNav';

const hoisted = vi.hoisted(() => {
    return {
        mockSearchParams: new URLSearchParams(),
    };
});

vi.mock('react-router-dom', () => ({
    useSearchParams: vi.fn(() => [hoisted.mockSearchParams]),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDataTableTagsQuery: vi.fn(() => ({
            data: {
                dataTableTags: [
                    {id: '1', name: 'Tag1'},
                    {id: '2', name: 'Tag2'},
                ],
            },
            isLoading: false,
        })),
    };
});

describe('useDataTablesLeftSidebarNav', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.mockSearchParams = new URLSearchParams();
    });

    describe('initial state', () => {
        it('returns tags from query', () => {
            const {result} = renderHook(() => useDataTablesLeftSidebarNav());

            expect(result.current.tags).toHaveLength(2);
            expect(result.current.tags[0].name).toBe('Tag1');
        });

        it('returns isLoading as false', () => {
            const {result} = renderHook(() => useDataTablesLeftSidebarNav());

            expect(result.current.isLoading).toBe(false);
        });

        it('returns tagId as null when no search params', () => {
            const {result} = renderHook(() => useDataTablesLeftSidebarNav());

            expect(result.current.tagId).toBeNull();
        });
    });

    describe('with tag filter', () => {
        it('returns tagId from search params', () => {
            hoisted.mockSearchParams = new URLSearchParams('tagId=1');

            const {result} = renderHook(() => useDataTablesLeftSidebarNav());

            expect(result.current.tagId).toBe('1');
        });
    });
});
