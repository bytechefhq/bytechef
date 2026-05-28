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
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by </span>

            {tagId ? (
                <>
                    <span className="text-sm text-muted-foreground uppercase">tag:</span>

                    <Badge
                        label={typeof pageTitle === 'string' ? pageTitle : 'Unknown Tag'}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                </>
            ) : (
                <span className="text-sm text-muted-foreground uppercase">none</span>
            )}
        </div>
    );
};

export default DataTablesFilterTitle;
