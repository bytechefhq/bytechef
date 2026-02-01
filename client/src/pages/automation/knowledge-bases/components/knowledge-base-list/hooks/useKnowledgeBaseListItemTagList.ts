import {
    KnowledgeBaseTagsEntry,
    Tag,
    TagInput,
    UpdateKnowledgeBaseTagsInput,
    useUpdateKnowledgeBaseTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

interface UpdateKnowledgeBaseTagsVarsI {
    input: UpdateKnowledgeBaseTagsInput;
}

interface UseKnowledgeBaseListItemTagListProps {
    knowledgeBaseId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

export default function useKnowledgeBaseListItemTagList({
    knowledgeBaseId,
    remainingTags,
    tags,
}: UseKnowledgeBaseListItemTagListProps) {
    const queryClient = useQueryClient();

    const updateTagsMutation = useUpdateKnowledgeBaseTagsMutation({
        onError: (_err, _vars, ctx) => {
            if (ctx?.previous) {
                queryClient.setQueryData(['knowledgeBaseTagsByKnowledgeBase'], ctx.previous);
            }
        },
        onMutate: async (variables: UpdateKnowledgeBaseTagsVarsI) => {
            await queryClient.cancelQueries({queryKey: ['knowledgeBaseTagsByKnowledgeBase']});

            const previous = queryClient.getQueryData<{knowledgeBaseTagsByKnowledgeBase: KnowledgeBaseTagsEntry[]}>([
                'knowledgeBaseTagsByKnowledgeBase',
            ]);

            const next = (() => {
                if (!previous?.knowledgeBaseTagsByKnowledgeBase) return previous;

                const withTempIds = (variables.input.tags ?? []).map((tag: TagInput) => ({
                    ...tag,
                    id: tag.id ?? -Math.floor(Date.now() + Math.random() * 1000),
                }));

                const updated = previous.knowledgeBaseTagsByKnowledgeBase.map((entry) =>
                    entry.knowledgeBaseId === knowledgeBaseId ? {...entry, tags: withTempIds} : entry
                );

                const hasEntry = previous.knowledgeBaseTagsByKnowledgeBase.some(
                    (entry) => entry.knowledgeBaseId === knowledgeBaseId
                );

                return hasEntry
                    ? {...previous, knowledgeBaseTagsByKnowledgeBase: updated}
                    : {
                          ...previous,
                          knowledgeBaseTagsByKnowledgeBase: [
                              ...previous.knowledgeBaseTagsByKnowledgeBase,
                              {knowledgeBaseId, tags: withTempIds},
                          ],
                      };
            })();

            queryClient.setQueryData(['knowledgeBaseTagsByKnowledgeBase'], next);

            return {previous};
        },
        onSettled: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseTags']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseTagsByKnowledgeBase']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
        },
    });

    const convertedTags = useMemo(() => tags.map((tag) => ({...tag, id: Number(tag.id)})), [tags]);

    const convertedRemainingTags = useMemo(
        () => remainingTags?.map((tag) => ({...tag, id: Number(tag.id)})),
        [remainingTags]
    );

    return {
        convertedRemainingTags,
        convertedTags,
        updateTagsMutation,
    };
}
