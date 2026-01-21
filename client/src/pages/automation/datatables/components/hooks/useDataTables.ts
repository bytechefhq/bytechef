import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    DataTable,
    DataTableTagsEntry,
    Tag,
    useDataTableTagsByTableQuery,
    useDataTableTagsQuery,
    useDataTablesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

interface UseDataTablesI {
    allTags: Tag[];
    error: unknown;
    filteredTables: DataTable[];
    isLoading: boolean;
    tables: DataTable[];
    tagId: string | undefined;
    tagsByTableData: DataTableTagsEntry[];
}

export default function useDataTables(): UseDataTablesI {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, error, isLoading} = useDataTablesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const tables = useMemo(() => data?.dataTables ?? [], [data?.dataTables]);

    const [searchParams] = useSearchParams();
    const tagIdParam = searchParams.get('tagId');
    const tagId = tagIdParam ?? undefined;

    const {data: tagsByTableQueryData} = useDataTableTagsByTableQuery();
    const {data: allTagsData} = useDataTableTagsQuery();

    const tagsByTableData = useMemo(
        () => tagsByTableQueryData?.dataTableTagsByTable ?? [],
        [tagsByTableQueryData?.dataTableTagsByTable]
    );
    const allTags = useMemo(() => allTagsData?.dataTableTags ?? [], [allTagsData?.dataTableTags]);

    const filteredTables = useMemo(() => {
        if (!tagId) return tables;

        const tableIdsWithTag = new Set<string>();

        for (const entry of tagsByTableData) {
            const hasTag = entry.tags?.some((tag) => tag.id === tagId);

            if (hasTag) tableIdsWithTag.add(entry.tableId as string);
        }

        return tables.filter((table) => tableIdsWithTag.has(table.id));
    }, [tables, tagsByTableData, tagId]);

    return {
        allTags: allTags as Tag[],
        error,
        filteredTables: filteredTables as DataTable[],
        isLoading,
        tables: tables as DataTable[],
        tagId,
        tagsByTableData: tagsByTableData as DataTableTagsEntry[],
    };
}
