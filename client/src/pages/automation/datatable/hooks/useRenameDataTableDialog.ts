import {useRenameDataTableDialogStore} from '@/pages/automation/datatable/stores/useRenameDataTableDialogStore';
import {useRenameDataTableMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

interface UseRenameDataTableDialogI {
    canRename: boolean;
    handleClose: () => void;
    handleOpen: (tableId: string, baseName: string) => void;
    handleOpenChange: (open: boolean) => void;
    handleRename: () => void;
    handleRenameValueChange: (value: string) => void;
    open: boolean;
    renameValue: string;
}

export default function useRenameDataTableDialog(): UseRenameDataTableDialogI {
    const {baseName, clearTableToRename, renameValue, setRenameValue, setTableToRename, tableIdToRename} =
        useRenameDataTableDialogStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const renameMutation = useRenameDataTableMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            clearTableToRename();
        },
    });

    const canRename = useMemo(
        () => renameValue.trim().length > 0 && renameValue.trim() !== baseName,
        [renameValue, baseName]
    );

    const handleClose = () => {
        clearTableToRename();
    };

    const handleOpen = (tableId: string, tableName: string) => {
        setTableToRename(tableId, tableName);
    };

    const handleRenameValueChange = (value: string) => {
        setRenameValue(value);
    };

    const handleRename = () => {
        if (tableIdToRename && canRename) {
            renameMutation.mutate({
                input: {
                    environmentId: String(environmentId),
                    newBaseName: renameValue.trim(),
                    tableId: tableIdToRename,
                },
            });
        }
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        canRename,
        handleClose,
        handleOpen,
        handleOpenChange,
        handleRename,
        handleRenameValueChange,
        open: tableIdToRename !== null,
        renameValue,
    };
}
