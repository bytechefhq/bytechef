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
    const queryClient = useQueryClient();

    const updateTagsMutation = useUpdateDataTableTagsMutation({
        onError: (_err, _vars, ctx) => {
            if (ctx?.previous) {
                queryClient.setQueryData(['dataTableTagsByTable'], ctx.previous);
            }
        },
        onMutate: async (variables: UpdateDataTableTagsMutationVariables) => {
            await queryClient.cancelQueries({queryKey: ['dataTableTagsByTable']});

            const previous = queryClient.getQueryData<{dataTableTagsByTable: DataTableTagsByTableEntryType[]}>([
                'dataTableTagsByTable',
            ]);

            const next = (() => {
                if (!previous?.dataTableTagsByTable) return previous;

                const withTempIds = (variables.input.tags ?? []).map((tag) => ({
                    ...tag,
                    id: tag.id ?? -Math.floor(Date.now() + Math.random() * 1000),
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

            queryClient.setQueryData(['dataTableTagsByTable'], next);

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
