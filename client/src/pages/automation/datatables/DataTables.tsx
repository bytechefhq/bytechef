import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CreateDataTableDialog from '@/pages/automation/datatables/components/CreateDataTableDialog';
import DataTableList from '@/pages/automation/datatables/components/DataTableList';
import DataTablesFilterTitle from '@/pages/automation/datatables/components/DataTablesFilterTitle';
import DataTablesLeftSidebarNav from '@/pages/automation/datatables/components/DataTablesLeftSidebarNav';
import useDataTables from '@/pages/automation/datatables/components/hooks/useDataTables';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import StorageUsageBanner from '@/shared/components/StorageUsageBanner';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useDataTableStorageUsageQuery} from '@/shared/middleware/graphql';
import {Table2Icon} from 'lucide-react';

const DataTables = () => {
    const {allTags, error, filteredTables, isLoading, tables, tagId, tagsByTableData} = useDataTables();

    const {data: storageUsageData} = useDataTableStorageUsageQuery();

    const storageUsage = storageUsageData?.dataTableStorageUsage;

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        tables.length > 0 ? (
                            <div className="flex items-center gap-4">
                                <EnvironmentSelect />

                                <CreateDataTableDialog trigger={<Button>New Table</Button>} />
                            </div>
                        ) : (
                            !isLoading && <EnvironmentSelect />
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
                        button={<CreateDataTableDialog trigger={<Button>Create Table</Button>} />}
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
