import {useCurrentDataTableStore} from '@/pages/automation/datatable/stores/useCurrentDataTableStore';
import {useDeleteDataTableRowsDialogStore} from '@/pages/automation/datatable/stores/useDeleteDataTableRowsDialogStore';
import {useSelectedRowsStore} from '@/pages/automation/datatable/stores/useSelectedRowsStore';
import {useDeleteDataTableRowMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseDeleteDataTableRowsDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    isPending: boolean;
    open: boolean;
    rowCount: number;
}

export default function useDeleteDataTableRowsDialog(): UseDeleteDataTableRowsDialogI {
    const [isPending, setIsPending] = useState(false);

    const {dataTable} = useCurrentDataTableStore();
    const {clearDialog, open, setOpen} = useDeleteDataTableRowsDialogStore();
    const {clearSelectedRows, selectedRows, setSelectedRows} = useSelectedRowsStore();
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
        const rowIds = Array.from(selectedRows);

        setIsPending(true);

        // Batch all deletions and invalidate once after all settle
        const results = await Promise.allSettled(
            rowIds.map((rowId) =>
                deleteRowMutation.mutateAsync({
                    input: {environmentId: String(environmentId), id: String(rowId), tableId},
                })
            )
        );

        setIsPending(false);

        queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});

        const failedRowIds = rowIds.filter((rowId, index) => results[index].status === 'rejected');

        if (failedRowIds.length === 0) {
            clearDialog();
            clearSelectedRows();
        } else {
            // Keep the dialog open with only the failed rows selected, so the count is accurate and a retry targets them
            setSelectedRows(new Set(failedRowIds));
        }
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
        isPending,
        open,
        rowCount: selectedRows.size,
    };
}
