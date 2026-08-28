import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import CreateDataTableDialog from '@/shared/components/data-tables/components/CreateDataTableDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useQueryClient} from '@tanstack/react-query';
import {Plus} from 'lucide-react';
import {useEffect} from 'react';
import {DataGrid} from 'react-data-grid';
import {useParams} from 'react-router-dom';

import 'react-data-grid/lib/styles.css';

import './DataTable.css';
import AddDataTableColumnDialog from './components/AddDataTableColumnDialog';
import DataTableHeader from './components/DataTableHeader';
import DataTableLeftSidebar from './components/DataTableLeftSidebar';
import DeleteDataTableAlertDialog from './components/DeleteDataTableAlertDialog';
import DeleteDataTableColumnDialog from './components/DeleteDataTableColumnDialog';
import DeleteDataTableRowsDialog from './components/DeleteDataTableRowsDialog';
import ImportDataTableCsvDialog from './components/ImportDataTableCsvDialog';
import RenameDataTableColumnDialog from './components/RenameDataTableColumnDialog';
import RenameDataTableDialog from './components/RenameDataTableDialog';
import {useDataTable} from './hooks/useDataTable';
import {useSelectedRowsStore} from './stores/useSelectedRowsStore';

const DataTable = () => {
    const {id} = useParams<{id: string}>();
    const {selectedRows} = useSelectedRowsStore();

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

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

    const openCopilot = () => {
        setContext({
            mode: MODE.ASK,
            parameters: {dataTableId: id},
            source: Source.DATA_TABLE,
        });

        setCopilotPanelOpen(true);
    };

    // Refresh this table's rows + the data table list after a BUILD-mode copilot turn mutates data (e.g. inserting
    // rows or adding a column), so the page reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.DATA_TABLE, () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            queryClient.invalidateQueries({queryKey: ['dataTableRowsPage']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={<DataTableHeader onAskCopilot={copilotEnabled ? openCopilot : undefined} />}
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

            <DeleteDataTableAlertDialog />
        </LayoutContainer>
    );
};

export default DataTable;
