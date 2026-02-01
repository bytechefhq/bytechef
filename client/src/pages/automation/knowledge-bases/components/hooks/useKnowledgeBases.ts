import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    KnowledgeBase,
    KnowledgeBaseTagsEntry,
    Tag,
    useKnowledgeBaseTagsByKnowledgeBaseQuery,
    useKnowledgeBaseTagsQuery,
    useKnowledgeBasesQuery,
} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

interface UseKnowledgeBasesResultI {
    allTags: Tag[];
    error: unknown;
    filteredKnowledgeBases: KnowledgeBase[];
    isLoading: boolean;
    knowledgeBases: KnowledgeBase[];
    tagId: string | undefined;
    tagsByKnowledgeBaseData: KnowledgeBaseTagsEntry[];
}

export default function useKnowledgeBases(): UseKnowledgeBasesResultI {
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, error, isLoading} = useKnowledgeBasesQuery({workspaceId: String(workspaceId)});

    const knowledgeBases = useMemo(
        () =>
            (data?.knowledgeBases ?? []).filter(
                (knowledgeBase): knowledgeBase is NonNullable<typeof knowledgeBase> => knowledgeBase !== null
            ),
        [data?.knowledgeBases]
    );

    const [searchParams] = useSearchParams();
    const tagIdParam = searchParams.get('tagId');
    const tagId = tagIdParam ?? undefined;

    const {data: tagsByKnowledgeBaseQueryData} = useKnowledgeBaseTagsByKnowledgeBaseQuery();
    const {data: allTagsData} = useKnowledgeBaseTagsQuery();

    const tagsByKnowledgeBaseData = useMemo(
        () => tagsByKnowledgeBaseQueryData?.knowledgeBaseTagsByKnowledgeBase ?? [],
        [tagsByKnowledgeBaseQueryData?.knowledgeBaseTagsByKnowledgeBase]
    );
    const allTags = useMemo(() => allTagsData?.knowledgeBaseTags ?? [], [allTagsData?.knowledgeBaseTags]);

    const filteredKnowledgeBases = useMemo(() => {
        if (!tagId) return knowledgeBases;

        const knowledgeBaseIdsWithTag = new Set<string>();

        for (const entry of tagsByKnowledgeBaseData) {
            const hasTag = entry.tags?.some((tag) => String(tag.id) === tagId);

            if (hasTag) knowledgeBaseIdsWithTag.add(entry.knowledgeBaseId);
        }

        return knowledgeBases.filter((knowledgeBase) => knowledgeBaseIdsWithTag.has(knowledgeBase.id));
    }, [knowledgeBases, tagsByKnowledgeBaseData, tagId]);

    return {
        allTags,
        error,
        filteredKnowledgeBases,
        isLoading,
        knowledgeBases,
        tagId,
        tagsByKnowledgeBaseData,
    };
}
