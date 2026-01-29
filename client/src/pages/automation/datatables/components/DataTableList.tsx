import DeleteDataTableAlertDialog from '@/pages/automation/datatable/components/DeleteDataTableAlertDialog';
import DataTableListItem from '@/pages/automation/datatables/components/DataTableListItem';
import DuplicateDataTableDialog from '@/pages/automation/datatables/components/DuplicateDataTableDialog';
import RenameDataTableDialog from '@/pages/automation/datatables/components/RenameDataTableDialog';
import useDataTableList from '@/pages/automation/datatables/components/hooks/useDataTableList';
import {DataTable, DataTableTagsEntry, Tag} from '@/shared/middleware/graphql';

interface DataTableListProps {
    allTags: Tag[];
    dataTables: DataTable[];
    tagsByTableData: DataTableTagsEntry[];
}

const DataTableList = ({allTags, dataTables, tagsByTableData}: DataTableListProps) => {
    const {sortedTables, tagsByTableMap} = useDataTableList({dataTables, tagsByTableData});

    return (
        <div className="w-full divide-y divide-border/50 px-4">
            {sortedTables.map((table) => {
                const currentTags = tagsByTableMap.get(table.id) || [];

                const currentTagIds = new Set(currentTags.map((tag) => tag.id));

                const remainingTags = allTags.filter((tag) => !currentTagIds.has(tag.id));

                return (
                    <DataTableListItem key={table.id} remainingTags={remainingTags} table={table} tags={currentTags} />
                );
            })}

            <DeleteDataTableAlertDialog />

            <DuplicateDataTableDialog />

            <RenameDataTableDialog />
        </div>
    );
};

export default DataTableList;
