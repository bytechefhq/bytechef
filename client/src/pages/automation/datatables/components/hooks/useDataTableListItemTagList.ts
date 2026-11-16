import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    DataTableTagsByTableQuery,
    UpdateDataTableTagsMutationVariables,
    useUpdateDataTableTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

type DataTableTagsByTableEntryType = DataTableTagsByTableQuery['dataTableTagsByTable'][number];

interface UseDataTableListItemTagListProps {
    tableId: string;
}

export default function useDataTableListItemTagList({tableId}: UseDataTableListItemTagListProps) {
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const tagsByTableQueryKey = ['dataTableTagsByTable', {workspaceId: String(workspaceId)}];

    const updateTagsMutation = useUpdateDataTableTagsMutation({
        onError: (_err, _vars, ctx) => {
            if (ctx?.previous) {
                queryClient.setQueryData(tagsByTableQueryKey, ctx.previous);
            }
        },
        onMutate: async (variables: UpdateDataTableTagsMutationVariables) => {
            await queryClient.cancelQueries({queryKey: ['dataTableTagsByTable']});

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
            void queryClient.invalidateQueries({queryKey: ['dataTableTags']});
            void queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});
            void queryClient.invalidateQueries({queryKey: ['dataTables']});
        },
    });

    return {
        updateTagsMutation,
    };
}
