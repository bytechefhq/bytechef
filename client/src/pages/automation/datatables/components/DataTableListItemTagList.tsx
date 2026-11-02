import useDataTableListItemTagList from '@/pages/automation/datatables/components/hooks/useDataTableListItemTagList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {Tag} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface DataTableListItemTagListProps {
    datatableId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

const convertTagToTagListFormat = (tag: Tag) => ({
    ...tag,
    id: tag.id ? Number(tag.id) : undefined,
});

const DataTableListItemTagList = ({datatableId, remainingTags, tags}: DataTableListItemTagListProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {updateTagsMutation} = useDataTableListItemTagList({tableId: datatableId});
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

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
            readOnly={!canEditDataTable}
            remainingTags={convertedRemainingTags}
            tags={convertedTags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default DataTableListItemTagList;
