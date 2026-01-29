import useKnowledgeBaseDocumentListItemTagList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItemTagList';
import TagList from '@/shared/components/TagList';
import {Tag} from '@/shared/middleware/graphql';

interface KnowledgeBaseDocumentListItemTagListProps {
    knowledgeBaseDocumentId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

const KnowledgeBaseDocumentListItemTagList = ({
    knowledgeBaseDocumentId,
    remainingTags,
    tags,
}: KnowledgeBaseDocumentListItemTagListProps) => {
    const {convertedRemainingTags, convertedTags, updateTagsMutation} = useKnowledgeBaseDocumentListItemTagList({
        knowledgeBaseDocumentId,
        remainingTags,
        tags,
    });

    return (
        <TagList
            getRequest={(_id, newTags) => ({
                input: {
                    knowledgeBaseDocumentId,
                    tags: newTags.map((tag) => ({id: tag.id, name: tag.name})),
                },
            })}
            id={+knowledgeBaseDocumentId}
            remainingTags={convertedRemainingTags}
            tags={convertedTags}
            updateTagsMutation={updateTagsMutation}
        />
    );
};

export default KnowledgeBaseDocumentListItemTagList;
