import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import CreateDataTableDialog from '@/pages/automation/datatables/components/CreateDataTableDialog';
import DataTableList from '@/pages/automation/datatables/components/DataTableList';
import DataTablesFilterTitle from '@/pages/automation/datatables/components/DataTablesFilterTitle';
import DataTablesLeftSidebarNav from '@/pages/automation/datatables/components/DataTablesLeftSidebarNav';
import useDataTables from '@/pages/automation/datatables/components/hooks/useDataTables';
import {DATA_TABLES_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useQueryClient} from '@tanstack/react-query';
import {Table2Icon} from 'lucide-react';
import {useEffect} from 'react';

const DataTables = () => {
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const {allTags, error, filteredTables, isLoading, tables, tagId, tagsByTableData} = useDataTables();

    const queryClient = useQueryClient();

    const initialLoadComplete = useInitialLoadComplete(isLoading);

    const isFirstRun = !isLoading && !tagId && tables.length === 0;

    useEffect(() => {
        return registerPostTurn(Source.DATA_TABLE, () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            queryClient.invalidateQueries({queryKey: ['dataTableTags']});
            queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        (tables.length > 0 || !isLoading) && (
                            <div className="flex items-center gap-1">
                                <CopilotButton source={Source.DATA_TABLE} />

                                {tables.length > 0 && <CreateDataTableDialog trigger={<Button>New Table</Button>} />}
                            </div>
                        )
                    }
                    title={
                        tables.length > 0 ? (
                            <DataTablesFilterTitle allTags={allTags} tagsByTableData={tagsByTableData} />
                        ) : (
                            isFirstRun && 'Data Tables'
                        )
                    }
                />
            }
            leftSidebarBody={<DataTablesLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Data Tables" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {filteredTables.length > 0 ? (
                    <DataTableList allTags={allTags} dataTables={filteredTables} tagsByTableData={tagsByTableData} />
                ) : tagId ? (
                    <EmptyList
                        icon={<Table2Icon className="size-24 text-stroke-neutral-tertiary" />}
                        message="No data tables match the selected tag."
                        title="No Matching Tables"
                    />
                ) : (
                    <EmptyState
                        actions={<CreateDataTableDialog trigger={<Button>Create Table</Button>} />}
                        description="A data table stores structured rows inside ByteChef, like lookup values, settings or records your workflows have already handled. Workflows can read and write it with no external database to set up."
                        docsLink={{href: DATA_TABLES_DOCUMENTATION_URL, label: 'How data tables work'}}
                        icon={<Table2Icon />}
                        title="Create your first data table"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default DataTables;
