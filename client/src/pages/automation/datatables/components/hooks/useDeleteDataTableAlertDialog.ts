import {useDeleteDataTableDialogStore} from '@/pages/automation/datatables/stores/useDeleteDataTableDialogStore';
import {useDropDataTableMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

interface UseDeleteDataTableAlertDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: (tableId: string) => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
}

export default function useDeleteDataTableAlertDialog(): UseDeleteDataTableAlertDialogI {
    const {clearTableIdToDelete, setTableIdToDelete, tableIdToDelete} = useDeleteDataTableDialogStore();

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const dropMutation = useDropDataTableMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['dataTables']});
            void queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});
            clearTableIdToDelete();
        },
    });

    const handleClose = () => {
        clearTableIdToDelete();
    };

    const handleOpen = (tableId: string) => {
        setTableIdToDelete(tableId);
    };

    const handleDelete = () => {
        if (tableIdToDelete) {
            dropMutation.mutate({input: {environmentId: String(environmentId), tableId: tableIdToDelete}});
        }
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        handleClose,
        handleDelete,
        handleOpen,
        handleOpenChange,
        open: tableIdToDelete !== null,
    };
}
