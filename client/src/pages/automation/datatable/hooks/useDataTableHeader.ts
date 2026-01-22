import {useSelectedRowsStore} from '../stores/useSelectedRowsStore';
import useDeleteDataTableRowsDialog from './useDeleteDataTableRowsDialog';

interface UseDataTableHeaderI {
    deleteRowsCount: number;
    deleteRowsDialogOpen: boolean;
    handleDeleteRows: () => void;
    handleDeleteRowsDialogOpenChange: (open: boolean) => void;
    handleOpenDeleteRowsDialog: () => void;
    selectedRowsCount: number;
}

export default function useDataTableHeader(): UseDataTableHeaderI {
    const {selectedRows} = useSelectedRowsStore();

    const {
        handleDelete: handleDeleteRows,
        handleOpen: handleOpenDeleteRowsDialog,
        handleOpenChange: handleDeleteRowsDialogOpenChange,
        open: deleteRowsDialogOpen,
        rowCount: deleteRowsCount,
    } = useDeleteDataTableRowsDialog();

    return {
        deleteRowsCount,
        deleteRowsDialogOpen,
        handleDeleteRows,
        handleDeleteRowsDialogOpenChange,
        handleOpenDeleteRowsDialog,
        selectedRowsCount: selectedRows.size,
    };
}
