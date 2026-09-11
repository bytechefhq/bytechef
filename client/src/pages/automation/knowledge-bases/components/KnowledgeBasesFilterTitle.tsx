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
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

            {tagId ? (
                <Badge
                    label={`Tags: ${typeof pageTitle === 'string' ? pageTitle : 'Unknown Tag'}`}
                    styleType="primary-outline"
                    weight="semibold"
                />
            ) : (
                <span className="text-sm text-muted-foreground uppercase">none</span>
            )}
        </div>
    );
};

export default KnowledgeBasesFilterTitle;
