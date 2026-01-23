import {useImportDataTableCsvMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import {useImportDataTableCsvDialogStore} from '../stores/useImportDataTableCsvDialogStore';

interface UseImportDataTableCsvDialogI {
    handleClose: () => void;
    handleImport: (csvContent: string) => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    isPending: boolean;
    open: boolean;
}

export default function useImportDataTableCsvDialog(): UseImportDataTableCsvDialogI {
    const {clearDialog, open, setOpen} = useImportDataTableCsvDialogStore();
    const {dataTable} = useCurrentDataTableStore();

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const importCsvMutation = useImportDataTableCsvMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});

            clearDialog();
        },
    });

    const handleClose = () => {
        clearDialog();
    };

    const handleOpen = () => {
        setOpen(true);
    };

    const handleImport = (csvContent: string) => {
        if (!dataTable?.id) return;

        importCsvMutation.mutate({
            input: {csv: csvContent, environmentId: String(environmentId), tableId: dataTable.id},
        });
    };

    const handleOpenChange = (openValue: boolean) => {
        if (!openValue) {
            handleClose();
        } else {
            setOpen(true);
        }
    };

    return {
        handleClose,
        handleImport,
        handleOpen,
        handleOpenChange,
        isPending: importCsvMutation.isPending,
        open,
    };
}
