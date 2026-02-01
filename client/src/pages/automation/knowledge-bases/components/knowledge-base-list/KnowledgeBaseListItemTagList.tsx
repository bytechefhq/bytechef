import useKnowledgeBaseListItemTagList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/hooks/useKnowledgeBaseListItemTagList';
import TagList from '@/shared/components/TagList';
import {Tag} from '@/shared/middleware/graphql';

interface KnowledgeBaseListItemTagListProps {
    knowledgeBaseId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

const KnowledgeBaseListItemTagList = ({knowledgeBaseId, remainingTags, tags}: KnowledgeBaseListItemTagListProps) => {
    const {convertedRemainingTags, convertedTags, updateTagsMutation} = useKnowledgeBaseListItemTagList({
        knowledgeBaseId,
        remainingTags,
        tags,
    });

    return (
        <TagList
            getRequest={(_id, newTags) => ({
                input: {
                    knowledgeBaseId,
                    tags: newTags.map((tag) => ({id: tag.id, name: tag.name})),
                },
            })}
            id={+knowledgeBaseId}
            remainingTags={convertedRemainingTags}
            tags={convertedTags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default KnowledgeBaseListItemTagList;
