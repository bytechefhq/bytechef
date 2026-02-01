import useKnowledgeBaseList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/hooks/useKnowledgeBaseList';
import {KnowledgeBase, KnowledgeBaseTagsEntry, Tag} from '@/shared/middleware/graphql';

import KnowledgeBaseListItem from './KnowledgeBaseListItem';

interface KnowledgeBaseListProps {
    allTags: Tag[];
    knowledgeBases: KnowledgeBase[];
    tagsByKnowledgeBaseData: KnowledgeBaseTagsEntry[];
}

const KnowledgeBaseList = ({allTags, knowledgeBases, tagsByKnowledgeBaseData}: KnowledgeBaseListProps) => {
    const {sortedKnowledgeBases, tagsByKnowledgeBaseMap} = useKnowledgeBaseList({
        knowledgeBases,
        tagsByKnowledgeBaseData,
    });

    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            {sortedKnowledgeBases.map((knowledgeBase) => {
                const currentTags = tagsByKnowledgeBaseMap.get(knowledgeBase.id) || [];

                const currentTagIds = new Set(currentTags.map((tag) => tag.id));

                const remainingTags = allTags.filter((tag) => !currentTagIds.has(tag.id));

                return (
                    <KnowledgeBaseListItem
                        key={knowledgeBase.id}
                        knowledgeBase={knowledgeBase}
                        remainingTags={remainingTags}
                        tags={currentTags}
                    />
                );
            })}
        </div>
    );
};

export default KnowledgeBaseList;
