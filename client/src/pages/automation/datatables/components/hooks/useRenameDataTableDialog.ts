import {useRenameDataTableDialogStore} from '@/pages/automation/datatables/stores/useRenameDataTableDialogStore';
import {useRenameDataTableMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

interface UseRenameDataTableDialogI {
    canRename: boolean;
    handleClose: () => void;
    handleOpen: (tableId: string, baseName: string) => void;
    handleOpenChange: (open: boolean) => void;
    handleRenameSubmit: () => void;
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

    const canRename = useMemo(() => {
        const trimmedValue = renameValue.trim();

        return trimmedValue.length > 0 && trimmedValue !== baseName;
    }, [renameValue, baseName]);

    const handleClose = () => {
        clearTableToRename();
    };

    const handleOpen = (tableId: string, baseName: string) => {
        setTableToRename(tableId, baseName);
    };

    const handleRenameValueChange = (value: string) => {
        setRenameValue(value);
    };

    const handleRenameSubmit = () => {
        if (tableIdToRename) {
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
        handleRenameSubmit,
        handleRenameValueChange,
        open: tableIdToRename !== null,
        renameValue,
    };
}
