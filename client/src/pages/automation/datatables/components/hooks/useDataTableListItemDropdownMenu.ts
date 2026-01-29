import {useToast} from '@/hooks/use-toast';
import useDeleteDataTableAlertDialog from '@/pages/automation/datatable/hooks/useDeleteDataTableAlertDialog';
import useDuplicateDataTableDialog from '@/pages/automation/datatables/components/hooks/useDuplicateDataTableDialog';
import useRenameDataTableDialog from '@/pages/automation/datatables/components/hooks/useRenameDataTableDialog';
import {useExportDataTableCsvQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {MouseEvent, useCallback} from 'react';

interface UseDataTableListItemDropdownMenuProps {
    dataTableId: string;
    baseName: string;
}

interface UseDataTableListItemDropdownMenuI {
    handleDeleteClick: (event: MouseEvent) => void;
    handleDuplicateClick: (event: MouseEvent) => void;
    handleExportCsvClick: (event: MouseEvent) => void;
    handleRenameClick: (event: MouseEvent) => void;
}

export default function useDataTableListItemDropdownMenu({
    baseName,
    dataTableId,
}: UseDataTableListItemDropdownMenuProps): UseDataTableListItemDropdownMenuI {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {toast} = useToast();

    const {handleOpen: handleOpenDeleteDataTableAlertDialog} = useDeleteDataTableAlertDialog();
    const {handleOpen: handleDuplicateDialogOpen} = useDuplicateDataTableDialog();
    const {handleOpen: handleRenameDialogOpen} = useRenameDataTableDialog();

    const {refetch: refetchExportCsv} = useExportDataTableCsvQuery(
        {environmentId: String(environmentId), tableId: dataTableId},
        {enabled: false}
    );

    const handleRenameClick = useCallback(
        (event: MouseEvent) => {
            event.preventDefault();
            event.stopPropagation();

            handleRenameDialogOpen(dataTableId, baseName);
        },
        [baseName, dataTableId, handleRenameDialogOpen]
    );

    const handleDuplicateClick = useCallback(
        (event: MouseEvent) => {
            event.preventDefault();
            event.stopPropagation();

            handleDuplicateDialogOpen(dataTableId, baseName);
        },
        [baseName, dataTableId, handleDuplicateDialogOpen]
    );

    const handleExportCsvClick = useCallback(
        async (event: MouseEvent) => {
            event.preventDefault();
            event.stopPropagation();

            try {
                const {data} = await refetchExportCsv();

                if (!data?.exportDataTableCsv) return;

                const blob = new Blob([data.exportDataTableCsv], {type: 'text/csv;charset=utf-8;'});
                const url = window.URL.createObjectURL(blob);
                const anchor = document.createElement('a');
                anchor.href = url;
                anchor.download = `${baseName}.csv`;
                anchor.click();
                window.URL.revokeObjectURL(url);
            } catch (error) {
                console.error('Failed to export CSV:', error);

                toast({
                    description: 'Failed to export CSV',
                    variant: 'destructive',
                });
            }
        },
        [baseName, refetchExportCsv, toast]
    );

    const handleDeleteClick = useCallback(
        (event: MouseEvent) => {
            event.preventDefault();
            event.stopPropagation();

            handleOpenDeleteDataTableAlertDialog(dataTableId, baseName);
        },
        [baseName, dataTableId, handleOpenDeleteDataTableAlertDialog]
    );

    return {
        handleDeleteClick,
        handleDuplicateClick,
        handleExportCsvClick,
        handleRenameClick,
    };
}
