import Button from '@/components/Button/Button';
import Header from '@/shared/layout/Header';
import {Trash2} from 'lucide-react';

import useDataTableActionsMenu from '../hooks/useDataTableActionsMenu';
import useDataTableHeader from '../hooks/useDataTableHeader';
import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import DataTableActionsMenu from './DataTableActionsMenu';

const DataTableHeader = () => {
    const {dataTable} = useCurrentDataTableStore();

    const {handleOpenDeleteRowsDialog, selectedRowsCount} = useDataTableHeader();

    const {handleExportCsv, handleOpenDeleteDialog, handleOpenImportCsvDialog, handleOpenRenameDialog} =
        useDataTableActionsMenu();

    return (
        <Header
            centerTitle
            position="main"
            right={
                <div className="flex items-center gap-2">
                    {selectedRowsCount > 0 && (
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
