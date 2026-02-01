import {KnowledgeBaseTagsEntry, Tag} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

interface UseKnowledgeBasesFilterTitleProps {
    allTags: Tag[];
    tagsByKnowledgeBaseData: KnowledgeBaseTagsEntry[];
}

interface UseKnowledgeBasesFilterTitleResultI {
    pageTitle: string | undefined;
    tagId: string | null;
}

export default function useKnowledgeBasesFilterTitle({
    allTags,
    tagsByKnowledgeBaseData,
}: UseKnowledgeBasesFilterTitleProps): UseKnowledgeBasesFilterTitleResultI {
    const [searchParams] = useSearchParams();

    const tagId = searchParams.get('tagId');

    const pageTitle = useMemo(() => {
        if (!tagId) return undefined;

        const fromGlobal = allTags.find((tag) => String(tag.id) === tagId)?.name;

        if (fromGlobal) return fromGlobal;

        const flat = tagsByKnowledgeBaseData.flatMap((entry) => entry.tags ?? []);

        return flat.find((tag) => String(tag.id) === tagId)?.name;
    }, [allTags, tagsByKnowledgeBaseData, tagId]);

    return {
        pageTitle,
        tagId,
    };
}
