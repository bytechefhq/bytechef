import {useExportDataTableCsvQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useCallback} from 'react';

import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import useDeleteDataTableAlertDialog from './useDeleteDataTableAlertDialog';
import useImportDataTableCsvDialog from './useImportDataTableCsvDialog';
import useRenameDataTableDialog from './useRenameDataTableDialog';

interface UseDataTableActionsMenuI {
    canRenameTable: boolean;
    deleteDialogOpen: boolean;
    deleteDialogTableName: string | null;
    handleDeleteDialogOpenChange: (open: boolean) => void;
    handleDeleteTable: () => void;
    handleExportCsv: () => void;
    handleImportCsv: (csvContent: string) => void;
    handleImportCsvDialogOpenChange: (open: boolean) => void;
    handleOpenDeleteDialog: () => void;
    handleOpenImportCsvDialog: () => void;
    handleOpenRenameDialog: () => void;
    handleRenameDialogOpenChange: (open: boolean) => void;
    handleRenameTable: () => void;
    handleRenameTableValueChange: (value: string) => void;
    importCsvDialogOpen: boolean;
    importCsvPending: boolean;
    renameDialogOpen: boolean;
    renameTableValue: string;
}

export default function useDataTableActionsMenu(): UseDataTableActionsMenuI {
    const {dataTable} = useCurrentDataTableStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        handleDelete: handleDeleteTable,
        handleOpen: openDeleteDialog,
        handleOpenChange: handleDeleteDialogOpenChange,
        open: deleteDialogOpen,
        tableName: deleteDialogTableName,
    } = useDeleteDataTableAlertDialog();

    const {
        canRename: canRenameTable,
        handleOpen: openRenameDialog,
        handleOpenChange: handleRenameDialogOpenChange,
        handleRename: handleRenameTable,
        handleRenameValueChange: handleRenameTableValueChange,
        open: renameDialogOpen,
        renameValue: renameTableValue,
    } = useRenameDataTableDialog();

    const {
        handleImport: handleImportCsv,
        handleOpen: openImportCsvDialog,
        handleOpenChange: handleImportCsvDialogOpenChange,
        isPending: importCsvPending,
        open: importCsvDialogOpen,
    } = useImportDataTableCsvDialog();

    const {refetch: refetchExportCsv} = useExportDataTableCsvQuery(
        {environmentId: String(environmentId), tableId: dataTable?.id ?? ''},
        {enabled: false}
    );

    const handleExportCsv = useCallback(async () => {
        if (!dataTable?.id) return;

        const {data} = await refetchExportCsv();

        if (!data?.exportDataTableCsv) return;

        const blob = new Blob([data.exportDataTableCsv], {type: 'text/csv;charset=utf-8;'});

        const url = URL.createObjectURL(blob);

        const anchor = document.createElement('a');

        anchor.href = url;
        anchor.download = `${dataTable.baseName}.csv`;

        document.body.appendChild(anchor);

        anchor.click();
        anchor.remove();

        URL.revokeObjectURL(url);
    }, [dataTable?.id, dataTable?.baseName, refetchExportCsv]);

    const handleOpenDeleteDialog = useCallback(() => {
        openDeleteDialog(dataTable?.id ?? '', dataTable?.baseName ?? '');
    }, [openDeleteDialog, dataTable?.id, dataTable?.baseName]);

    const handleOpenRenameDialog = useCallback(() => {
        openRenameDialog(dataTable?.id ?? '', dataTable?.baseName ?? '');
    }, [openRenameDialog, dataTable?.id, dataTable?.baseName]);

    const handleOpenImportCsvDialog = useCallback(() => {
        openImportCsvDialog();
    }, [openImportCsvDialog]);

    return {
        canRenameTable,
        deleteDialogOpen,
        deleteDialogTableName,
        handleDeleteDialogOpenChange,
        handleDeleteTable,
        handleExportCsv,
        handleImportCsv,
        handleImportCsvDialogOpenChange,
        handleOpenDeleteDialog,
        handleOpenImportCsvDialog,
        handleOpenRenameDialog,
        handleRenameDialogOpenChange,
        handleRenameTable,
        handleRenameTableValueChange,
        importCsvDialogOpen,
        importCsvPending,
        renameDialogOpen,
        renameTableValue,
    };
}
