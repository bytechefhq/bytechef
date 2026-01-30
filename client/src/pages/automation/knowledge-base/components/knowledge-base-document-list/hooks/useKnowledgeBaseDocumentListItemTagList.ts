import {
    KnowledgeBaseDocumentTagsEntry,
    Tag,
    TagInput,
    UpdateKnowledgeBaseDocumentTagsInput,
    useUpdateKnowledgeBaseDocumentTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

interface UpdateKnowledgeBaseDocumentTagsVarsI {
    input: UpdateKnowledgeBaseDocumentTagsInput;
}

interface UseKnowledgeBaseDocumentListItemTagListProps {
    knowledgeBaseDocumentId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

export default function useKnowledgeBaseDocumentListItemTagList({
    knowledgeBaseDocumentId,
    remainingTags,
    tags,
}: UseKnowledgeBaseDocumentListItemTagListProps) {
    const queryClient = useQueryClient();

    const updateTagsMutation = useUpdateKnowledgeBaseDocumentTagsMutation({
        onError: (_err, _vars, ctx) => {
            if (ctx?.previous) {
                queryClient.setQueryData(['knowledgeBaseDocumentTagsByDocument'], ctx.previous);
            }
        },
        onMutate: async (variables: UpdateKnowledgeBaseDocumentTagsVarsI) => {
            await queryClient.cancelQueries({queryKey: ['knowledgeBaseDocumentTagsByDocument']});

            const previous = queryClient.getQueryData<{
                knowledgeBaseDocumentTagsByDocument: KnowledgeBaseDocumentTagsEntry[];
            }>(['knowledgeBaseDocumentTagsByDocument']);

            const next = (() => {
                if (!previous?.knowledgeBaseDocumentTagsByDocument) return previous;

                const withTempIds = (variables.input.tags ?? []).map((tag: TagInput) => ({
                    ...tag,
                    id: tag.id ?? -Math.floor(Date.now() + Math.random() * 1000),
                }));

                const updated = previous.knowledgeBaseDocumentTagsByDocument.map((entry) =>
                    entry.knowledgeBaseDocumentId === knowledgeBaseDocumentId ? {...entry, tags: withTempIds} : entry
                );

                const hasEntry = previous.knowledgeBaseDocumentTagsByDocument.some(
                    (entry) => entry.knowledgeBaseDocumentId === knowledgeBaseDocumentId
                );

                return hasEntry
                    ? {...previous, knowledgeBaseDocumentTagsByDocument: updated}
                    : {
                          ...previous,
                          knowledgeBaseDocumentTagsByDocument: [
                              ...previous.knowledgeBaseDocumentTagsByDocument,
                              {knowledgeBaseDocumentId, tags: withTempIds},
                          ],
                      };
            })();

            queryClient.setQueryData(['knowledgeBaseDocumentTagsByDocument'], next);

            return {previous};
        },
        onSettled: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseDocumentTags']});
            queryClient.invalidateQueries({queryKey: ['knowledgeBaseDocumentTagsByDocument']});
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
