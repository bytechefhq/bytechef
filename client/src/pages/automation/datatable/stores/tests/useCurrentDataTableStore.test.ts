import {ColumnType} from '@/shared/middleware/graphql';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useCurrentDataTableStore} from '../useCurrentDataTableStore';

describe('useCurrentDataTableStore', () => {
    beforeEach(() => {
        useCurrentDataTableStore.getState().clearDataTable();
    });

    afterEach(() => {
        useCurrentDataTableStore.getState().clearDataTable();
    });

    describe('initial state', () => {
        it('should have undefined dataTable initially', () => {
            const {dataTable} = useCurrentDataTableStore.getState();

            expect(dataTable).toBeUndefined();
        });
    });

    describe('setDataTable', () => {
        it('should set the dataTable', () => {
            const mockDataTable = {
                baseName: 'TestTable',
                columns: [{id: 'col1', name: 'Column 1', type: 'STRING' as ColumnType}],
                id: 'table-123',
            };

            useCurrentDataTableStore.getState().setDataTable(mockDataTable);

            const {dataTable} = useCurrentDataTableStore.getState();

            expect(dataTable).toEqual(mockDataTable);
        });

        it('should replace existing dataTable when setting a new one', () => {
            const firstTable = {
                baseName: 'FirstTable',
                columns: [] as {id: string; name: string; type: ColumnType}[],
                id: 'table-1',
            };
            const secondTable = {
                baseName: 'SecondTable',
                columns: [{id: 'col1', name: 'Column 1', type: 'NUMBER' as ColumnType}],
                id: 'table-2',
            };

            useCurrentDataTableStore.getState().setDataTable(firstTable);
            useCurrentDataTableStore.getState().setDataTable(secondTable);

            const {dataTable} = useCurrentDataTableStore.getState();

            expect(dataTable).toEqual(secondTable);
        });

        it('should handle dataTable with multiple columns', () => {
            const mockDataTable = {
                baseName: 'MultiColumnTable',
                columns: [
                    {id: 'col1', name: 'Name', type: 'STRING' as ColumnType},
                    {id: 'col2', name: 'Age', type: 'NUMBER' as ColumnType},
                    {id: 'col3', name: 'Active', type: 'BOOLEAN' as ColumnType},
                ],
                id: 'table-multi',
            };

            useCurrentDataTableStore.getState().setDataTable(mockDataTable);

            const {dataTable} = useCurrentDataTableStore.getState();

            expect(dataTable).toEqual(mockDataTable);
            expect(dataTable?.columns).toHaveLength(3);
        });
    });

    describe('clearDataTable', () => {
        it('should clear the dataTable to undefined', () => {
            const mockDataTable = {
                baseName: 'TestTable',
                columns: [] as {id: string; name: string; type: ColumnType}[],
                id: 'table-123',
            };

            useCurrentDataTableStore.getState().setDataTable(mockDataTable);
            useCurrentDataTableStore.getState().clearDataTable();

            const {dataTable} = useCurrentDataTableStore.getState();

            expect(dataTable).toBeUndefined();
        });

        it('should be idempotent when called multiple times', () => {
            useCurrentDataTableStore.getState().clearDataTable();
            useCurrentDataTableStore.getState().clearDataTable();

            const {dataTable} = useCurrentDataTableStore.getState();

            expect(dataTable).toBeUndefined();
        });
    });
});
