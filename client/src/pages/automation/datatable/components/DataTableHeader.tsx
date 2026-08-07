import Button from '@/components/Button/Button';
import Header from '@/shared/layout/Header';
import {SparklesIcon, Trash2Icon} from 'lucide-react';

import useDataTableActionsMenu from '../hooks/useDataTableActionsMenu';
import useDataTableHeader from '../hooks/useDataTableHeader';
import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';
import DataTableActionsMenu from './DataTableActionsMenu';

interface DataTableHeaderPropsI {
    onAskCopilot?: () => void;
}

const DataTableHeader = ({onAskCopilot}: DataTableHeaderPropsI) => {
    const {dataTable} = useCurrentDataTableStore();

    const {handleOpenDeleteRowsDialog, selectedRowsCount} = useDataTableHeader();

    const {handleExportCsv, handleOpenDeleteDialog, handleOpenImportCsvDialog, handleOpenRenameDialog} =
        useDataTableActionsMenu();

    return (
        <Header
            centerTitle
            position="main"
            right={
                <div className="flex items-center gap-1">
                    {selectedRowsCount > 0 && (
                        <Button onClick={handleOpenDeleteRowsDialog} variant="destructive">
                            <Trash2Icon className="size-4" /> Delete ({selectedRowsCount})
                        </Button>
                    )}

                    {onAskCopilot && (
                        <Button
                            aria-label="Ask Copilot"
                            icon={<SparklesIcon />}
                            onClick={onAskCopilot}
                            size="icon"
                            variant="ghost"
                        />
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
