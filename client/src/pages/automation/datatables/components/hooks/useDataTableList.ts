import {DataTable, DataTableTagsEntry, Tag} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface UseDataTableListProps {
    dataTables: DataTable[];
    tagsByTableData: DataTableTagsEntry[];
}

interface UseDataTableListI {
    sortedTables: DataTable[];
    tagsByTableMap: Map<string, Tag[]>;
}

export default function useDataTableList({dataTables, tagsByTableData}: UseDataTableListProps): UseDataTableListI {
    const tagsByTableMap = useMemo(() => {
        const map = new Map<string, Tag[]>();

        tagsByTableData.forEach((entry) => {
            map.set(entry.tableId, entry.tags || []);
        });

        return map;
    }, [tagsByTableData]);

    const collator = useMemo(() => new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}), []);

    const sortedTables = useMemo(() => {
        return [...dataTables].sort((tableA, tableB) =>
            collator.compare(tableA.baseName.trim(), tableB.baseName.trim())
        );
    }, [dataTables, collator]);

    return {
        sortedTables,
        tagsByTableMap,
    };
}
