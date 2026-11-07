import Button from '@/components/Button/Button';
import {Breadcrumb, BreadcrumbItem, BreadcrumbList} from '@/components/ui/breadcrumb';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
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
            position="main"
            right={
                <div className="flex items-center gap-2">
                    {selectedRowsCount > 0 && (
                        <Button onClick={handleOpenDeleteRowsDialog} variant="destructive">
                            <Trash2 className="size-4" /> Delete ({selectedRowsCount})
                        </Button>
                    )}

                    <CopilotButton
                        parameters={dataTable?.id ? {dataTableId: String(dataTable.id)} : {}}
                        source={Source.DATA_TABLE}
                    />

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
                <Breadcrumb className="min-w-0">
                    <BreadcrumbList className="flex-nowrap">
                        <ListPageBreadcrumbItem label="Data Tables" to="/automation/datatables" />

                        <BreadcrumbItem className="min-w-0 text-content-neutral-primary">
                            <h1 className="truncate font-semibold">{dataTable?.baseName}</h1>
                        </BreadcrumbItem>
                    </BreadcrumbList>
                </Breadcrumb>
            }
        />
    );
};

export default DataTableHeader;
