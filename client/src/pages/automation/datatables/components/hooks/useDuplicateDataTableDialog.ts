import {useDuplicateDataTableDialogStore} from '@/pages/automation/datatables/stores/useDuplicateDataTableDialogStore';
import {useDuplicateDataTableMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

interface UseDuplicateDataTableDialogI {
    canDuplicate: boolean;
    duplicateValue: string;
    handleClose: () => void;
    handleDuplicateSubmit: () => void;
    handleDuplicateValueChange: (value: string) => void;
    handleOpen: (tableId: string, baseName: string) => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
}

export default function useDuplicateDataTableDialog(): UseDuplicateDataTableDialogI {
    const {clearTableToDuplicate, duplicateValue, setDuplicateValue, setTableToDuplicate, tableIdToDuplicate} =
        useDuplicateDataTableDialogStore();

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const duplicateMutation = useDuplicateDataTableMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            clearTableToDuplicate();
        },
    });

    const canDuplicate = useMemo(() => duplicateValue.trim().length > 0, [duplicateValue]);

    const handleClose = () => {
        clearTableToDuplicate();
    };

    const handleOpen = (tableId: string, baseName: string) => {
        setTableToDuplicate(tableId, baseName);
    };

    const handleDuplicateValueChange = (value: string) => {
        setDuplicateValue(value);
    };

    const handleDuplicateSubmit = () => {
        if (tableIdToDuplicate) {
            duplicateMutation.mutate({
                input: {
                    environmentId: String(environmentId),
                    newBaseName: duplicateValue.trim(),
                    tableId: tableIdToDuplicate,
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
        canDuplicate,
        duplicateValue,
        handleClose,
        handleDuplicateSubmit,
        handleDuplicateValueChange,
        handleOpen,
        handleOpenChange,
        open: tableIdToDuplicate !== null,
    };
}
