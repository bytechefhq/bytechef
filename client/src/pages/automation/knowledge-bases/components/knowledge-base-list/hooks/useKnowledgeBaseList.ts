import {KnowledgeBase, KnowledgeBaseTagsEntry, Tag} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface UseKnowledgeBaseListProps {
    knowledgeBases: KnowledgeBase[];
    tagsByKnowledgeBaseData: KnowledgeBaseTagsEntry[];
}

interface UseKnowledgeBaseListResultI {
    sortedKnowledgeBases: KnowledgeBase[];
    tagsByKnowledgeBaseMap: Map<string, Tag[]>;
}

export default function useKnowledgeBaseList({
    knowledgeBases,
    tagsByKnowledgeBaseData,
}: UseKnowledgeBaseListProps): UseKnowledgeBaseListResultI {
    const tagsByKnowledgeBaseMap = useMemo(() => {
        const map = new Map<string, Tag[]>();

        tagsByKnowledgeBaseData.forEach((entry) => {
            map.set(entry.knowledgeBaseId, entry.tags || []);
        });

        return map;
    }, [tagsByKnowledgeBaseData]);

    const collator = useMemo(() => new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}), []);

    const sortedKnowledgeBases = useMemo(() => {
        return [...knowledgeBases].sort((knowledgeBaseA, knowledgeBaseB) =>
            collator.compare(knowledgeBaseA.name.trim(), knowledgeBaseB.name.trim())
        );
    }, [knowledgeBases, collator]);

    return {
        sortedKnowledgeBases,
        tagsByKnowledgeBaseMap,
    };
}
