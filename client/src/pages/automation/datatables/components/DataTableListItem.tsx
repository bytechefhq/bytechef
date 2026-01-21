import DataTableListItemDropdownMenu from '@/pages/automation/datatables/components/DataTableListItemDropdownMenu';
import DataTableListItemTagList from '@/pages/automation/datatables/components/DataTableListItemTagList';
import DataTableListItemTooltip from '@/pages/automation/datatables/components/DataTableListItemTooltip';
import DuplicateDataTableAlertDialog from '@/pages/automation/datatables/components/DuplicateDataTableAlertDialog';
import RenameDataTableAlertDialog from '@/pages/automation/datatables/components/RenameDataTableAlertDialog';
import useDataTableListItem from '@/pages/automation/datatables/components/hooks/useDataTableListItem';
import {DataTable, Tag} from '@/shared/middleware/graphql';

interface DataTableListItemProps {
    table: DataTable;
    tags: Tag[];
    remainingTags?: Tag[];
}

const DataTableListItem = ({remainingTags, table, tags}: DataTableListItemProps) => {
    const {columnCountLabel, handleDataTableListItemTagListClick, handleRowClick} = useDataTableListItem({table});

    return (
        <>
            <div
                className="group flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-destructive-foreground"
                onClick={handleRowClick}
            >
                <div className="flex flex-1 items-center py-5">
                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            <span className="text-base font-semibold">{table.baseName}</span>
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center gap-3">
                                <div className="text-xs font-semibold text-muted-foreground">{columnCountLabel}</div>

                                <div onClick={handleDataTableListItemTagListClick}>
                                    <DataTableListItemTagList
                                        datatableId={table.id}
                                        remainingTags={remainingTags}
                                        tags={tags}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <DataTableListItemTooltip lastModifiedDate={table.lastModifiedDate} />

                        <div onClick={handleDataTableListItemTagListClick}>
                            <DataTableListItemDropdownMenu baseName={table.baseName} dataTableId={table.id} />
                        </div>
                    </div>
                </div>
            </div>

            <DuplicateDataTableAlertDialog />

            <RenameDataTableAlertDialog />
        </>
    );
};

export default DataTableListItem;
