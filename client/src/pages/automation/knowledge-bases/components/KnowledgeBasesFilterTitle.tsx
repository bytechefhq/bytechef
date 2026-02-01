import Badge from '@/components/Badge/Badge';
import useKnowledgeBasesFilterTitle from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBasesFilterTitle';
import {KnowledgeBaseTagsEntry, Tag} from '@/shared/middleware/graphql';

interface KnowledgeBasesFilterTitleProps {
    allTags: Tag[];
    tagsByKnowledgeBaseData: KnowledgeBaseTagsEntry[];
}

const KnowledgeBasesFilterTitle = ({allTags, tagsByKnowledgeBaseData}: KnowledgeBasesFilterTitleProps) => {
    const {pageTitle, tagId} = useKnowledgeBasesFilterTitle({allTags, tagsByKnowledgeBaseData});

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

export default KnowledgeBasesFilterTitle;
