import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
import {useUpdateContextStoreTagsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface ContextStoreListItemTagListProps {
    contextStoreId: string;
    remainingTags: {id: number; name: string}[];
    tags: {id: number; name: string}[];
}

const ContextStoreListItemTagList = ({contextStoreId, remainingTags, tags}: ContextStoreListItemTagListProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const updateTagsMutation = useUpdateContextStoreTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['contextStores']});
            queryClient.invalidateQueries({queryKey: ['contextStoreTags']});
        },
    });

    return (
        <TagList
            getRequest={(_id, newTags) => ({
                id: contextStoreId,
                tags: newTags.map((tag) => ({
                    id: tag.id != null ? String(tag.id) : undefined,
                    name: tag.name,
                })),
                workspaceId: String(currentWorkspaceId),
            })}
            id={Number(contextStoreId)}
            remainingTags={remainingTags}
            tags={tags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default ContextStoreListItemTagList;
