import {useRemoveDataTableColumnMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import {useDeleteDataTableColumnDialogStore} from '../stores/useDeleteDataTableColumnDialogStore';

interface UseDeleteDataTableColumnDialogI {
    columnName: string | null;
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: (columnId: string, columnName: string) => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
}

export default function useDeleteDataTableColumnDialog(): UseDeleteDataTableColumnDialogI {
    const {clearDialog, columnId, columnName, setColumnToDelete} = useDeleteDataTableColumnDialogStore();
    const {dataTable} = useCurrentDataTableStore();

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId) ?? 2;

    const queryClient = useQueryClient();

    const removeColumnMutation = useRemoveDataTableColumnMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            clearDialog();
        },
    });

    const handleClose = () => {
        clearDialog();
    };

    const handleOpen = (colId: string, colName: string) => {
        setColumnToDelete(colId, colName);
    };

    const handleDelete = () => {
        if (!dataTable?.id || !columnId) return;

        removeColumnMutation.mutate({
            input: {columnId, environmentId: String(environmentId), tableId: dataTable.id},
        });
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        columnName,
        handleClose,
        handleDelete,
        handleOpen,
        handleOpenChange,
        open: columnId !== null,
    };
}
