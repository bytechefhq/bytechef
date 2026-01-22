import PageLoader from '@/components/PageLoader';
import {Input} from '@/components/ui/input';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';

import useDataTableLeftSidebar from '../hooks/useDataTableLeftSidebar';
import DataTableLeftSidebarDropdownMenu from './DataTableLeftSidebarDropdownMenu';
import DeleteDataTableDialog from './DeleteDataTableDialog';
import RenameDataTableDialog from './RenameDataTableDialog';

interface Props {
    currentId?: string;
}

const DataTableLeftSidebar = ({currentId}: Props) => {
    const {error, filteredTables, handleSearchChange, isLoading, search} = useDataTableLeftSidebar();

    return (
        <div className="flex h-full flex-col">
            <div className="space-y-2 px-4 pb-3 pt-0.5">
                <Input
                    onChange={(event) => handleSearchChange(event.target.value)}
                    placeholder="Search tables..."
                    value={search}
                />
            </div>

            <div className="flex-1 overflow-y-auto">
                <PageLoader errors={[error]} loading={isLoading}>
                    <LeftSidebarNav
                        body={
                            <>
                                {filteredTables.map((table) => {
                                    const active = currentId != null ? table.id === currentId : false;

                                    return (
                                        <div className="group relative flex items-center" key={table.id}>
                                            <LeftSidebarNavItem
                                                item={{current: active, name: table.baseName}}
                                                toLink={`/automation/datatables/${table.id}`}
                                            />

                                            <div className="absolute right-2 top-1/2 z-10 -translate-y-1/2">
                                                <DataTableLeftSidebarDropdownMenu
                                                    tableId={table.id}
                                                    tableName={table.baseName}
                                                />
                                            </div>
                                        </div>
                                    );
                                })}

                                {filteredTables.length === 0 && (
                                    <div className="px-2 py-2 text-sm text-muted-foreground">No tables found</div>
                                )}
                            </>
                        }
                    />
                </PageLoader>
            </div>

            <DeleteDataTableDialog />

            <RenameDataTableDialog />
        </div>
    );
};

export default DataTableLeftSidebar;
