import {endpointUrl, fetchParams} from '@/shared/middleware/config';
import {
    DataTableRowsPageDocument,
    DataTableRowsPageQuery,
    DataTableRowsPageQueryVariables,
} from '@/shared/middleware/graphql';
import {InfiniteData, UseInfiniteQueryOptions, useInfiniteQuery} from '@tanstack/react-query';

export const DataTableKeys = {
    list: (environmentId: string, workspaceId: string) => ['dataTables', {environmentId, workspaceId}] as const,
    rows: (environmentId: string, tableId: string) => ['dataTableRows', {environmentId, tableId}] as const,
    rowsPage: (environmentId: string, tableId: string, limit = 100) =>
        ['dataTableRowsPage', {environmentId, limit, tableId}] as const,
};

async function gqlFetch<TData, TVars>(query: string, variables?: TVars): Promise<TData> {
    const res = await fetch(endpointUrl as string, {
        method: 'POST',
        ...(fetchParams as RequestInit),
        body: JSON.stringify({query, variables}),
    });

    const json = await res.json();

    if (json.errors) {
        throw new Error(json.errors[0]?.message ?? 'GraphQL error');
    }

    return json.data;
}

export const useDataTableRowsInfiniteQuery = (
    variables: {environmentId: string; tableId: string; limit?: number},
    options?: Omit<
        UseInfiniteQueryOptions<DataTableRowsPageQuery, Error, InfiniteData<DataTableRowsPageQuery, number>>,
        'queryKey' | 'queryFn' | 'getNextPageParam' | 'initialPageParam'
    >
) => {
    const limit = variables.limit ?? 100;

    return useInfiniteQuery({
        getNextPageParam: (lastPage) => {
            const page = lastPage.dataTableRowsPage;

            return page.hasMore ? (page.nextOffset ?? undefined) : undefined;
        },
        initialPageParam: 0,
        queryFn: ({pageParam}) =>
            gqlFetch<DataTableRowsPageQuery, DataTableRowsPageQueryVariables>(DataTableRowsPageDocument, {
                environmentId: variables.environmentId,
                limit,
                offset: typeof pageParam === 'number' ? pageParam : 0,
                tableId: variables.tableId,
            }),
        queryKey: DataTableKeys.rowsPage(variables.environmentId, variables.tableId, limit),
        ...options,
    });
};
