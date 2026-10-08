import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CreateDataTableDialog from '@/pages/automation/datatables/components/CreateDataTableDialog';
import DataTableList from '@/pages/automation/datatables/components/DataTableList';
import DataTablesFilterTitle from '@/pages/automation/datatables/components/DataTablesFilterTitle';
import DataTablesLeftSidebarNav from '@/pages/automation/datatables/components/DataTablesLeftSidebarNav';
import useDataTables from '@/pages/automation/datatables/components/hooks/useDataTables';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Table2Icon} from 'lucide-react';

const DataTables = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {allTags, error, filteredTables, isLoading, tables, tagId, tagsByTableData} = useDataTables();
    const canCreateDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_CREATE');

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        tables.length > 0 &&
                        canCreateDataTable && <CreateDataTableDialog trigger={<Button>New Table</Button>} />
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
                {filteredTables.length > 0 ? (
                    <DataTableList allTags={allTags} dataTables={filteredTables} tagsByTableData={tagsByTableData} />
                ) : (
                    <EmptyList
                        button={
                            canCreateDataTable ? (
                                <CreateDataTableDialog trigger={<Button>Create Table</Button>} />
                            ) : undefined
                        }
                        icon={<Table2Icon className="size-24 text-gray-300" />}
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
