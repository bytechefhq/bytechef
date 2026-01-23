import {useRenameDataTableColumnMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import {useRenameDataTableColumnDialogStore} from '../stores/useRenameDataTableColumnDialogStore';

interface UseRenameDataTableColumnDialogI {
    canRename: boolean;
    currentName: string;
    handleClose: () => void;
    handleOpen: (columnId: string, currentName: string) => void;
    handleOpenChange: (open: boolean) => void;
    handleRename: () => void;
    handleRenameValueChange: (value: string) => void;
    open: boolean;
    renameValue: string;
}

export default function useRenameDataTableColumnDialog(): UseRenameDataTableColumnDialogI {
    const {clearDialog, columnId, currentName, renameValue, setColumnToRename, setRenameValue} =
        useRenameDataTableColumnDialogStore();
    const {dataTable} = useCurrentDataTableStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const renameColumnMutation = useRenameDataTableColumnMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});

            clearDialog();
        },
    });

    const canRename = useMemo(() => {
        const trimmed = renameValue.trim();

        return trimmed.length > 0 && trimmed !== currentName && trimmed.toLowerCase() !== 'id';
    }, [renameValue, currentName]);

    const handleClose = () => {
        clearDialog();
    };

    const handleOpen = (colId: string, colName: string) => {
        setColumnToRename(colId, colName);
    };

    const handleRenameValueChange = (value: string) => {
        setRenameValue(value);
    };

    const handleRename = () => {
        if (!dataTable?.id || !columnId || !canRename) return;

        renameColumnMutation.mutate({
            input: {columnId, environmentId: String(environmentId), newName: renameValue.trim(), tableId: dataTable.id},
        });
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        canRename,
        currentName,
        handleClose,
        handleOpen,
        handleOpenChange,
        handleRename,
        handleRenameValueChange,
        open: columnId !== null,
        renameValue,
    };
}
