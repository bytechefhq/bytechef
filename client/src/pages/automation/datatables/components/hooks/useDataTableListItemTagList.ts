import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    DataTableTagsByTableQuery,
    UpdateDataTableTagsMutationVariables,
    useUpdateDataTableTagsMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

type DataTableTagsByTableEntryType = DataTableTagsByTableQuery['dataTableTagsByTable'][number];

interface UseDataTableListItemTagListProps {
    tableId: string;
}

export default function useDataTableListItemTagList({tableId}: UseDataTableListItemTagListProps) {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const tagsByTableQueryKey = [
        'dataTableTagsByTable',
        {environmentId: String(environmentId), workspaceId: String(workspaceId)},
    ];

    const updateTagsMutation = useUpdateDataTableTagsMutation({
        onError: (_err, _vars, ctx) => {
            if (ctx?.previous) {
                queryClient.setQueryData(tagsByTableQueryKey, ctx.previous);
            }
        },
        onMutate: async (variables: UpdateDataTableTagsMutationVariables) => {
            await queryClient.cancelQueries({queryKey: tagsByTableQueryKey});

            const previous = queryClient.getQueryData<{dataTableTagsByTable: DataTableTagsByTableEntryType[]}>(
                tagsByTableQueryKey
            );

            const next = (() => {
                if (!previous?.dataTableTagsByTable) return previous;

                const withTempIds = (variables.input.tags ?? []).map((tag) => ({
                    ...tag,
                    id: tag.id,
                }));

                const updated = previous.dataTableTagsByTable.map((entry) =>
                    entry.tableId === tableId ? {...entry, tags: withTempIds} : entry
                );

                const hasEntry = previous.dataTableTagsByTable.some((entry) => entry.tableId === tableId);

                return hasEntry
                    ? {...previous, dataTableTagsByTable: updated}
                    : {
                          ...previous,
                          dataTableTagsByTable: [...previous.dataTableTagsByTable, {tableId, tags: withTempIds}],
                      };
            })();

            queryClient.setQueryData(tagsByTableQueryKey, next);

            return {previous};
        },
        onSettled: () => {
            queryClient.invalidateQueries({queryKey: ['dataTableTags']});
            queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});
            queryClient.invalidateQueries({queryKey: ['dataTables']});
        },
    });

    return {
        updateTagsMutation,
    };
}
