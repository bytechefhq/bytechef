import {useCurrentDataTableStore} from '@/pages/automation/datatable/stores/useCurrentDataTableStore';
import {useDeleteDataTableRowsDialogStore} from '@/pages/automation/datatable/stores/useDeleteDataTableRowsDialogStore';
import {useSelectedRowsStore} from '@/pages/automation/datatable/stores/useSelectedRowsStore';
import {useDeleteDataTableRowMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

interface UseDeleteDataTableRowsDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
    rowCount: number;
}

export default function useDeleteDataTableRowsDialog(): UseDeleteDataTableRowsDialogI {
    const {dataTable} = useCurrentDataTableStore();
    const {clearDialog, open, setOpen} = useDeleteDataTableRowsDialogStore();
    const {clearSelectedRows, selectedRows} = useSelectedRowsStore();

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const deleteRowMutation = useDeleteDataTableRowMutation({});

    const handleClose = () => {
        clearDialog();
    };

    const handleOpen = () => {
        setOpen();
    };

    const handleDelete = async () => {
        if (!selectedRows || selectedRows.size === 0) return;

        if (!dataTable?.id) return;

        const tableId = dataTable.id;

        // Batch all deletions and invalidate once after all complete
        await Promise.all(
            Array.from(selectedRows).map((rowId) =>
                deleteRowMutation.mutateAsync({
                    input: {environmentId: String(environmentId), id: String(rowId), tableId},
                })
            )
        );

        queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});
        clearDialog();
        clearSelectedRows();
    };

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) {
            handleClose();
        }
    };

    return {
        handleClose,
        handleDelete,
        handleOpen,
        handleOpenChange,
        open,
        rowCount: selectedRows.size,
    };
}
