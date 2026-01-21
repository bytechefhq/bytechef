import useDeleteDataTableAlertDialog from '@/pages/automation/datatables/components/hooks/useDeleteDataTableAlertDialog';
import useDuplicateDataTableAlertDialog from '@/pages/automation/datatables/components/hooks/useDuplicateDataTableAlertDialog';
import useRenameDataTableAlertDialog from '@/pages/automation/datatables/components/hooks/useRenameDataTableAlertDialog';
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
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId) ?? 2;

    const {handleOpen: handleOpenDeleteDataTableAlertDialog} = useDeleteDataTableAlertDialog();
    const {handleOpen: handleDuplicateDialogOpen} = useDuplicateDataTableAlertDialog();
    const {handleOpen: handleRenameDialogOpen} = useRenameDataTableAlertDialog();

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
            } catch {
                alert('Failed to export CSV');
            }
        },
        [baseName, refetchExportCsv]
    );

    const handleDeleteClick = useCallback(
        (event: MouseEvent) => {
            event.preventDefault();
            event.stopPropagation();

            handleOpenDeleteDataTableAlertDialog(dataTableId);
        },
        [dataTableId, handleOpenDeleteDataTableAlertDialog]
    );

    return {
        handleDeleteClick,
        handleDuplicateClick,
        handleExportCsvClick,
        handleRenameClick,
    };
}
