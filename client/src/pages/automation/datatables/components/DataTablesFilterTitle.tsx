import Badge from '@/components/Badge/Badge';
import useDataTablesFilterTitle from '@/pages/automation/datatables/components/hooks/useDataTablesFilterTitle';
import {DataTableTagsEntry, Tag} from '@/shared/middleware/graphql';

interface DataTablesFilterTitleProps {
    allTags: Tag[];
    tagsByTableData: DataTableTagsEntry[];
}

const DataTablesFilterTitle = ({allTags, tagsByTableData}: DataTablesFilterTitleProps) => {
    const {pageTitle, tagId} = useDataTablesFilterTitle({allTags, tagsByTableData});

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            {tagId ? (
                <>
                    <span className="text-sm uppercase text-muted-foreground">tag:</span>

                    <Badge
                        label={typeof pageTitle === 'string' ? pageTitle : 'Unknown Tag'}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                </>
            ) : (
                <span className="text-sm uppercase text-muted-foreground">none</span>
            )}
        </div>
    );
};

export default DataTablesFilterTitle;
