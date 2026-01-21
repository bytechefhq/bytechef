import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTables from '../useDataTables';

const hoisted = vi.hoisted(() => {
    return {
        mockSearchParams: new URLSearchParams(),
    };
});

vi.mock('react-router-dom', () => ({
    useSearchParams: vi.fn(() => [hoisted.mockSearchParams]),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 2),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 1049),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDataTableTagsByTableQuery: vi.fn(() => ({
            data: {
                dataTableTagsByTable: [
                    {tableId: '1', tags: [{id: '1', name: 'Tag1'}]},
                    {tableId: '2', tags: [{id: '2', name: 'Tag2'}]},
                ],
            },
        })),
        useDataTableTagsQuery: vi.fn(() => ({
            data: {
                dataTableTags: [
                    {id: '1', name: 'Tag1'},
                    {id: '2', name: 'Tag2'},
                ],
            },
        })),
        useDataTablesQuery: vi.fn(() => ({
            data: {
                dataTables: [
                    {baseName: 'Table1', columns: [], id: '1', lastModifiedDate: '2024-01-01'},
                    {baseName: 'Table2', columns: [], id: '2', lastModifiedDate: '2024-01-02'},
                    {baseName: 'Table3', columns: [], id: '3', lastModifiedDate: '2024-01-03'},
                ],
            },
            error: null,
            isLoading: false,
        })),
    };
});

describe('useDataTables', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.mockSearchParams = new URLSearchParams();
    });

    describe('initial state', () => {
        it('returns tables from query', () => {
            const {result} = renderHook(() => useDataTables());

            expect(result.current.tables).toHaveLength(3);
        });

        it('returns isLoading as false', () => {
            const {result} = renderHook(() => useDataTables());

            expect(result.current.isLoading).toBe(false);
        });

        it('returns error as null', () => {
            const {result} = renderHook(() => useDataTables());

            expect(result.current.error).toBeNull();
        });
    });

    describe('without tag filter', () => {
        it('returns all tables as filteredTables', () => {
            const {result} = renderHook(() => useDataTables());

            expect(result.current.filteredTables).toHaveLength(3);
        });

        it('returns tagId as undefined', () => {
            const {result} = renderHook(() => useDataTables());

            expect(result.current.tagId).toBeUndefined();
        });
    });

    describe('with tag filter', () => {
        it('returns tagId from search params', () => {
            hoisted.mockSearchParams = new URLSearchParams('tagId=1');

            const {result} = renderHook(() => useDataTables());

            expect(result.current.tagId).toBe('1');
        });

        it('filters tables by tag', () => {
            hoisted.mockSearchParams = new URLSearchParams('tagId=1');

            const {result} = renderHook(() => useDataTables());

            expect(result.current.filteredTables).toHaveLength(1);
            expect(result.current.filteredTables[0].id).toBe('1');
        });
    });
});
