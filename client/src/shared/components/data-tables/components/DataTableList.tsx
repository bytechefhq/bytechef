import DeleteDataTableAlertDialog from '@/pages/automation/datatable/components/DeleteDataTableAlertDialog';
import DataTableListItem from '@/shared/components/data-tables/components/DataTableListItem';
import DuplicateDataTableDialog from '@/shared/components/data-tables/components/DuplicateDataTableDialog';
import RenameDataTableDialog from '@/shared/components/data-tables/components/RenameDataTableDialog';
import useDataTableList from '@/shared/components/data-tables/components/hooks/useDataTableList';
import {DataTable, DataTableTagsEntry, Tag} from '@/shared/middleware/graphql';

interface DataTableListProps {
    allTags: Tag[];
    dataTables: DataTable[];
    tagsByTableData: DataTableTagsEntry[];
}

const DataTableList = ({allTags, dataTables, tagsByTableData}: DataTableListProps) => {
    const {sortedTables, tagsByTableMap} = useDataTableList({dataTables, tagsByTableData});

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
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
