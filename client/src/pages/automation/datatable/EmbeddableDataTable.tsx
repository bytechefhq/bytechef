import PageLoader from '@/components/PageLoader';
import {DataGrid} from 'react-data-grid';

import 'react-data-grid/lib/styles.css';

import './DataTable.css';
import AddDataTableColumnDialog from './components/AddDataTableColumnDialog';
import DeleteDataTableAlertDialog from './components/DeleteDataTableAlertDialog';
import DeleteDataTableColumnDialog from './components/DeleteDataTableColumnDialog';
import DeleteDataTableRowsDialog from './components/DeleteDataTableRowsDialog';
import ImportDataTableCsvDialog from './components/ImportDataTableCsvDialog';
import RenameDataTableColumnDialog from './components/RenameDataTableColumnDialog';
import RenameDataTableDialog from './components/RenameDataTableDialog';
import {useDataTable} from './hooks/useDataTable';
import {useSelectedRowsStore} from './stores/useSelectedRowsStore';

interface EmbeddableDataTablePropsI {
    dataTableId: string;
}

/**
 * A self-contained data-table grid suitable for embedding inside panels such
 * as the AI Hub resource panel. Renders the full DataGrid with all
 * column/row management dialogs but without page-level chrome (no LayoutContainer,
 * no left sidebar, no page header).
 *
 * Differences from the full DataTable.tsx page:
 * - No left sidebar listing all tables
 * - No page header with table actions menu / bulk-delete button
 * - No workspace-level navigation chrome
 */
const EmbeddableDataTable = ({dataTableId}: EmbeddableDataTablePropsI) => {
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
    } = useDataTable({tableId: dataTableId});

    return (
        <div className="flex size-full flex-col">
            <PageLoader errors={[tablesError, rowsError]} loading={tablesLoading || rowsLoading}>
                <div className="grid size-full px-4">
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

            <DeleteDataTableAlertDialog />
        </div>
    );
};

export default EmbeddableDataTable;
