import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import Header from '@/shared/layout/Header';
import {Trash2} from 'lucide-react';

import useDataTableActionsMenu from '../hooks/useDataTableActionsMenu';
import useDataTableHeader from '../hooks/useDataTableHeader';
import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import DataTableActionsMenu from './DataTableActionsMenu';

const DataTableHeader = () => {
    const {dataTable} = useCurrentDataTableStore();
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {handleOpenDeleteRowsDialog, selectedRowsCount} = useDataTableHeader();

    const {handleExportCsv, handleOpenDeleteDialog, handleOpenImportCsvDialog, handleOpenRenameDialog} =
        useDataTableActionsMenu();
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

    return (
        <Header
            centerTitle
            position="main"
            right={
                <div className="flex items-center gap-2">
                    {selectedRowsCount > 0 && canEditDataTable && (
                        <Button onClick={handleOpenDeleteRowsDialog} variant="destructive">
                            <Trash2 className="size-4" /> Delete ({selectedRowsCount})
                        </Button>
                    )}

                    <DataTableActionsMenu
                        onDeleteTable={handleOpenDeleteDialog}
                        onExportCsv={handleExportCsv}
                        onImportCsv={handleOpenImportCsvDialog}
                        onRenameTable={handleOpenRenameDialog}
                        tableId={dataTable?.id}
                    />
                </div>
            }
            title={
                <div className="flex items-center gap-2">
                    <span className="font-semibold">{dataTable?.baseName}</span>
                </div>
            }
        />
    );
};

export default DataTableHeader;
