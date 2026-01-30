import {useDeleteDataTableAlertDialogStore} from '@/pages/automation/datatable/stores/useDeleteDataTableAlertDialogStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useDataTablesQuery, useDropDataTableMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';

interface UseDeleteDataTableAlertDialogI {
    handleClose: () => void;
    handleDelete: () => void;
    handleOpen: (tableId: string, tableName: string) => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
    tableName: string | null;
}

export default function useDeleteDataTableAlertDialog(): UseDeleteDataTableAlertDialogI {
    const {clearTableToDelete, setTableToDelete, tableIdToDelete, tableNameToDelete} =
        useDeleteDataTableAlertDialogStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const {data: tablesData} = useDataTablesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const dropMutation = useDropDataTableMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['dataTables']});
            void queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});

            const collator = new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'});
            const sorted = [...(tablesData?.dataTables ?? [])].sort((sortedTableA, sortedTableB) =>
                collator.compare(sortedTableA.baseName.trim(), sortedTableB.baseName.trim())
            );
            const currentIndex = sorted.findIndex((sortedTable) => sortedTable.id === tableIdToDelete);
            const nextTable = currentIndex >= 0 ? sorted[currentIndex + 1] : undefined;

            clearTableToDelete();

            if (nextTable?.id) {
                navigate(`/automation/datatables/${nextTable.id}`);
            } else {
                navigate('/automation/datatables');
            }
        },
    });

    const handleClose = () => {
        clearTableToDelete();
    };

    const handleOpen = (tableId: string, tableName: string) => {
        setTableToDelete(tableId, tableName);
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
        tableName: tableNameToDelete,
    };
}
