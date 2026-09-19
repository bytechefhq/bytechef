import useKnowledgeBaseListItemTagList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/hooks/useKnowledgeBaseListItemTagList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {Tag} from '@/shared/middleware/graphql';

interface KnowledgeBaseListItemTagListProps {
    knowledgeBaseId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

const KnowledgeBaseListItemTagList = ({knowledgeBaseId, remainingTags, tags}: KnowledgeBaseListItemTagListProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {convertedRemainingTags, convertedTags, updateTagsMutation} = useKnowledgeBaseListItemTagList({
        knowledgeBaseId,
        remainingTags,
        tags,
    });

    const canEditKnowledgeBase = useHasWorkspaceScope(currentWorkspaceId, 'KNOWLEDGE_BASE_EDIT');

    return (
        <TagList
            getRequest={(_id, newTags) => ({
                input: {
                    knowledgeBaseId,
                    tags: newTags.map((tag) => ({id: tag.id, name: tag.name})),
                },
            })}
            id={+knowledgeBaseId}
            readOnly={!canEditKnowledgeBase}
            remainingTags={convertedRemainingTags}
            tags={convertedTags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default KnowledgeBaseListItemTagList;
