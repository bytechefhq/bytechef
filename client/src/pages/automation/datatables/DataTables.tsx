import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import StorageUsageBanner from '@/shared/components/StorageUsageBanner';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import CreateDataTableDialog from '@/shared/components/data-tables/components/CreateDataTableDialog';
import DataTableList from '@/shared/components/data-tables/components/DataTableList';
import DataTablesFilterTitle from '@/shared/components/data-tables/components/DataTablesFilterTitle';
import DataTablesLeftSidebarNav from '@/shared/components/data-tables/components/DataTablesLeftSidebarNav';
import useDataTables from '@/shared/components/data-tables/components/hooks/useDataTables';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useDataTableStorageUsageQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {Table2Icon} from 'lucide-react';
import {useEffect} from 'react';

const DataTables = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {allTags, error, filteredTables, isLoading, tables, tagId, tagsByTableData} = useDataTables({
        type: 'WORKSPACE',
        workspaceId: currentWorkspaceId,
    });

    const {data: storageUsageData} = useDataTableStorageUsageQuery();

    const storageUsage = storageUsageData?.dataTableStorageUsage;

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    // Refresh the list and the tag sidebar after a BUILD-mode copilot turn creates or retags a table, so the page
    // reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.DATA_TABLE, () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            queryClient.invalidateQueries({queryKey: ['dataTableTags']});
            queryClient.invalidateQueries({queryKey: ['dataTableTagsByTable']});
            queryClient.invalidateQueries({queryKey: ['DataTableStorageUsage']});
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
                                <EnvironmentSelect />

                                <CopilotButton source={Source.DATA_TABLE} />

                                {tables.length > 0 && (
                                    // This is the "Create data table" command's target.
                                    <CreateDataTableDialog
                                        claimsCreateIntent={true}
                                        trigger={<Button>New Table</Button>}
                                    />
                                )}
                            </div>
                        )
                    }
                    title={
                        tables.length > 0 ? (
                            <DataTablesFilterTitle allTags={allTags} tagsByTableData={tagsByTableData} />
                        ) : (
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={<DataTablesLeftSidebarNav />}
            leftSidebarHeader={<Header position="sidebar" title="Data Tables" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {storageUsage && (
                    <StorageUsageBanner
                        label="Data table"
                        limitBytes={storageUsage.limitBytes}
                        percentage={storageUsage.percentage}
                        unlimited={storageUsage.unlimited}
                        usedBytes={storageUsage.usedBytes}
                    />
                )}

                {filteredTables.length > 0 ? (
                    <DataTableList allTags={allTags} dataTables={filteredTables} tagsByTableData={tagsByTableData} />
                ) : (
                    <EmptyList
                        button={
                            // This is the "Create data table" command's target.
                            <CreateDataTableDialog claimsCreateIntent={true} trigger={<Button>Create Table</Button>} />
                        }
                        icon={<Table2Icon className="size-24 text-stroke-neutral-tertiary" />}
                        message={
                            tagId
                                ? 'No data tables match the selected tag.'
                                : 'Get started by creating a new data table.'
                        }
                        title={tagId ? 'No Matching Tables' : 'No Data Tables'}
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default DataTables;
