import {KnowledgeBaseScopeType} from '@/shared/components/knowledge-bases/types';
import {
    KnowledgeBase,
    KnowledgeBaseTagsEntry,
    Tag,
    useEmbeddedKnowledgeBasesQuery,
    useKnowledgeBaseTagsByKnowledgeBaseQuery,
    useKnowledgeBaseTagsQuery,
    useKnowledgeBasesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
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

/**
 * The list behind both surfaces. See `@/shared/components/data-tables/components/hooks/useDataTables` for why both
 * queries are always called and why the tag reads are gated on the scope rather than on `enabled`.
 */
export default function useKnowledgeBases(scope: KnowledgeBaseScopeType): UseKnowledgeBasesResultI {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const isWorkspaceScope = scope.type === 'WORKSPACE';
    const workspaceId = scope.type === 'WORKSPACE' ? scope.workspaceId : undefined;
    const ownerId = scope.type === 'EMBEDDED' ? scope.ownerId : undefined;

    const {
        data: workspaceData,
        error: workspaceError,
        isLoading: workspaceIsLoading,
    } = useKnowledgeBasesQuery(
        {environmentId: String(environmentId), workspaceId: String(workspaceId)},
        {enabled: isWorkspaceScope}
    );

    const {
        data: embeddedData,
        error: embeddedError,
        isLoading: embeddedIsLoading,
    } = useEmbeddedKnowledgeBasesQuery(
        {environmentId: String(environmentId), ownerId: ownerId === undefined ? undefined : String(ownerId)},
        {enabled: !isWorkspaceScope}
    );

    const error = isWorkspaceScope ? workspaceError : embeddedError;
    const isLoading = isWorkspaceScope ? workspaceIsLoading : embeddedIsLoading;

    const knowledgeBases = useMemo(
        () =>
            (isWorkspaceScope
                ? (workspaceData?.knowledgeBases ?? [])
                : (embeddedData?.embeddedKnowledgeBases ?? [])
            ).filter((knowledgeBase): knowledgeBase is NonNullable<typeof knowledgeBase> => knowledgeBase !== null),
        [embeddedData?.embeddedKnowledgeBases, isWorkspaceScope, workspaceData?.knowledgeBases]
    );

    const [searchParams] = useSearchParams();
    const tagIdParam = searchParams.get('tagId');
    const tagId = tagIdParam ?? undefined;

    const {data: tagsByKnowledgeBaseQueryData} = useKnowledgeBaseTagsByKnowledgeBaseQuery(undefined, {
        enabled: isWorkspaceScope,
    });
    const {data: allTagsData} = useKnowledgeBaseTagsQuery(
        {workspaceId: String(workspaceId)},
        {enabled: isWorkspaceScope}
    );

    // Gated on the scope, not merely on `enabled` -- a disabled query still serves whatever is cached under its key.
    const tagsByKnowledgeBaseData = useMemo(
        () => (isWorkspaceScope ? (tagsByKnowledgeBaseQueryData?.knowledgeBaseTagsByKnowledgeBase ?? []) : []),
        [isWorkspaceScope, tagsByKnowledgeBaseQueryData?.knowledgeBaseTagsByKnowledgeBase]
    );
    const allTags = useMemo(
        () => (isWorkspaceScope ? (allTagsData?.knowledgeBaseTags ?? []) : []),
        [allTagsData?.knowledgeBaseTags, isWorkspaceScope]
    );

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
