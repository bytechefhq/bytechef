import {DataTableScopeType} from '@/shared/components/data-tables/types';
import {
    DataTable,
    DataTableTagsEntry,
    Tag,
    useDataTableTagsByTableQuery,
    useDataTableTagsQuery,
    useDataTablesQuery,
    useEmbeddedDataTablesQuery,
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

/**
 * The list behind both surfaces.
 *
 * Every query is called on every render with an `enabled` flag, rather than one branch of an `if`: hooks cannot be
 * called conditionally, and the disabled one costs no request.
 *
 * The workspace store is read by the automation page and passed in, which is the whole reason this hook is usable
 * from `/embedded` — a store read here would have coupled the component tree to a surface that has no workspaces.
 */
export default function useDataTables(scope: DataTableScopeType): UseDataTablesI {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    const isWorkspaceScope = scope.type === 'WORKSPACE';
    const workspaceId = scope.type === 'WORKSPACE' ? scope.workspaceId : undefined;
    const ownerId = scope.type === 'EMBEDDED' ? scope.ownerId : undefined;

    const {
        data: workspaceData,
        error: workspaceError,
        isLoading: workspaceIsLoading,
    } = useDataTablesQuery(
        {environmentId: String(environmentId), workspaceId: String(workspaceId)},
        {enabled: isWorkspaceScope}
    );

    const {
        data: embeddedData,
        error: embeddedError,
        isLoading: embeddedIsLoading,
    } = useEmbeddedDataTablesQuery(
        {
            environmentId: String(environmentId),
            ownerId: ownerId === undefined ? undefined : String(ownerId),
        },
        {enabled: !isWorkspaceScope}
    );

    const {data: tagsByTableQueryData} = useDataTableTagsByTableQuery(undefined, {enabled: isWorkspaceScope});
    const {data: allTagsData} = useDataTableTagsQuery({workspaceId: String(workspaceId)}, {enabled: isWorkspaceScope});

    const tagIdParam = searchParams.get('tagId');
    const tagId = tagIdParam ?? undefined;

    const tables = useMemo(
        () => (isWorkspaceScope ? (workspaceData?.dataTables ?? []) : (embeddedData?.embeddedDataTables ?? [])),
        [embeddedData?.embeddedDataTables, isWorkspaceScope, workspaceData?.dataTables]
    );

    const error = isWorkspaceScope ? workspaceError : embeddedError;
    const isLoading = isWorkspaceScope ? workspaceIsLoading : embeddedIsLoading;

    // Gated on the scope, not merely on `enabled`. A disabled query still serves whatever is cached under its key, so
    // an admin who opens the automation page and then the console would otherwise see that workspace's tags there.
    const tagsByTableData = useMemo(
        () => (isWorkspaceScope ? (tagsByTableQueryData?.dataTableTagsByTable ?? []) : []),
        [isWorkspaceScope, tagsByTableQueryData?.dataTableTagsByTable]
    );
    const allTags = useMemo(
        () => (isWorkspaceScope ? (allTagsData?.dataTableTags ?? []) : []),
        [allTagsData?.dataTableTags, isWorkspaceScope]
    );

    const filteredTables = useMemo(() => {
        if (!tagId) {
            return tables;
        }

        const tableIdsWithTag = new Set<string>();

        for (const entry of tagsByTableData) {
            const hasTag = entry.tags?.some((tag) => tag.id === tagId);

            if (hasTag) {
                tableIdsWithTag.add(entry.tableId as string);
            }
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
