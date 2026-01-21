import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTableList from '../useDataTableList';

const mockTables = [
    {baseName: 'Table 10', columns: [], id: '1', lastModifiedDate: 1704067200000},
    {baseName: 'Table 2', columns: [], id: '2', lastModifiedDate: 1704153600000},
    {baseName: 'Table 1', columns: [], id: '3', lastModifiedDate: 1704240000000},
];

const mockTagsByTableData = [
    {tableId: '1', tags: [{id: '1', name: 'Tag1'}]},
    {tableId: '2', tags: [{id: '2', name: 'Tag2'}]},
];

describe('useDataTableList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('sorted tables', () => {
        it('sorts tables by baseName in natural order', () => {
            const {result} = renderHook(() =>
                useDataTableList({dataTables: mockTables, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.sortedTables[0].baseName).toBe('Table 1');
            expect(result.current.sortedTables[1].baseName).toBe('Table 2');
            expect(result.current.sortedTables[2].baseName).toBe('Table 10');
        });
    });

    describe('tags by table map', () => {
        it('returns tags map', () => {
            const {result} = renderHook(() =>
                useDataTableList({dataTables: mockTables, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.tagsByTableMap.get('1')).toHaveLength(1);
            expect(result.current.tagsByTableMap.get('1')?.[0].name).toBe('Tag1');
        });
    });
});
