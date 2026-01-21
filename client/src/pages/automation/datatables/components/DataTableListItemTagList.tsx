import useDataTableListItemTagList from '@/pages/automation/datatables/components/hooks/useDataTableListItemTagList';
import TagList from '@/shared/components/TagList';
import {Tag} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface DataTableListItemTagListProps {
    datatableId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

// Convert Tag with string id to TagList's expected format with number id
const convertTagToTagListFormat = (tag: Tag) => ({
    ...tag,
    id: tag.id ? Number(tag.id) : undefined,
});

const DataTableListItemTagList = ({datatableId, remainingTags, tags}: DataTableListItemTagListProps) => {
    const {updateTagsMutation} = useDataTableListItemTagList({tableId: datatableId});

    const convertedTags = useMemo(() => tags.map(convertTagToTagListFormat), [tags]);

    const convertedRemainingTags = useMemo(() => remainingTags?.map(convertTagToTagListFormat), [remainingTags]);

    return (
        <TagList
            getRequest={(_id, newTags) => ({
                input: {
                    tableId: datatableId,
                    tags: newTags.map((tag) => ({id: tag.id ? String(tag.id) : undefined, name: tag.name})),
                },
            })}
            id={+datatableId}
            remainingTags={convertedRemainingTags}
            tags={convertedTags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default DataTableListItemTagList;
