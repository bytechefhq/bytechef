import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import CreateDataTableDialog from '@/pages/automation/datatables/components/CreateDataTableDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Plus} from 'lucide-react';
import {DataGrid} from 'react-data-grid';
import {useParams} from 'react-router-dom';

import 'react-data-grid/lib/styles.css';

import './DataTable.css';
import AddDataTableColumnDialog from './components/AddDataTableColumnDialog';
import DataTableHeader from './components/DataTableHeader';
import DataTableLeftSidebar from './components/DataTableLeftSidebar';
import DeleteDataTableColumnDialog from './components/DeleteDataTableColumnDialog';
import DeleteDataTableDialog from './components/DeleteDataTableDialog';
import DeleteDataTableRowsDialog from './components/DeleteDataTableRowsDialog';
import ImportDataTableCsvDialog from './components/ImportDataTableCsvDialog';
import RenameDataTableColumnDialog from './components/RenameDataTableColumnDialog';
import RenameDataTableDialog from './components/RenameDataTableDialog';
import {useDataTable} from './hooks/useDataTable';
import {useSelectedRowsStore} from './stores/useSelectedRowsStore';

const DataTable = () => {
    const {id} = useParams<{id: string}>();
    const {selectedRows} = useSelectedRowsStore();

    const {
        gridColumns,
        gridRowsWithAdd,
        handleGridScroll,
        handleRowsChange,
        handleSelectedRowsChange,
        rowsError,
        rowsLoading,
        tablesError,
        tablesLoading,
    } = useDataTable({tableId: id});

    return (
        <LayoutContainer
            header={<DataTableHeader />}
            leftSidebarBody={<DataTableLeftSidebar currentId={id} />}
            leftSidebarHeader={
                <Header
                    position="sidebar"
                    right={
                        <CreateDataTableDialog
                            trigger={
                                <Button
                                    aria-label="Create table"
                                    icon={<Plus className="h-4 w-4" />}
                                    size="icon"
                                    variant="ghost"
                                />
                            }
                        />
                    }
                    title="Data Tables"
                />
            }
            leftSidebarWidth="64"
        >
            <PageLoader errors={[tablesError, rowsError]} loading={tablesLoading || rowsLoading}>
                <div className="grid px-4">
                    <DataGrid
                        aria-description="Data Table"
                        bottomSummaryRows={[{}]}
                        columns={gridColumns}
                        onRowsChange={handleRowsChange}
                        onScroll={handleGridScroll}
                        onSelectedRowsChange={handleSelectedRowsChange}
                        rowKeyGetter={(row) => row.id}
                        rows={gridRowsWithAdd}
                        selectedRows={selectedRows}
                        style={{blockSize: '100%'}}
                    />
                </div>
            </PageLoader>

            <ImportDataTableCsvDialog />

            <AddDataTableColumnDialog />

            <RenameDataTableColumnDialog />

            <DeleteDataTableColumnDialog />

            <DeleteDataTableRowsDialog />

            <RenameDataTableDialog />

            <DeleteDataTableDialog />
        </LayoutContainer>
    );
};

export default DataTable;
