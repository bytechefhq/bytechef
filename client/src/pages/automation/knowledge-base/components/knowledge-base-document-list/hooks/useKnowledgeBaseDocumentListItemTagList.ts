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
        onError: (_error, _variables, context) => {
            if (context?.cachedTagsByDocumentData) {
                queryClient.setQueryData(['knowledgeBaseDocumentTagsByDocument'], context.cachedTagsByDocumentData);
            }
        },
        onMutate: async (variables: UpdateKnowledgeBaseDocumentTagsVarsI) => {
            await queryClient.cancelQueries({queryKey: ['knowledgeBaseDocumentTagsByDocument']});

            const previousTagsData = queryClient.getQueryData<{
                knowledgeBaseDocumentTagsByDocument: KnowledgeBaseDocumentTagsEntry[];
            }>(['knowledgeBaseDocumentTagsByDocument']);

            const optimisticTagsData = (() => {
                if (!previousTagsData?.knowledgeBaseDocumentTagsByDocument) {
                    return previousTagsData;
                }

                const tagsWithTempIds = (variables.input.tags ?? []).map((tag: TagInput, index: number) => ({
                    ...tag,
                    id: tag.id ?? -(Date.now() + index),
                }));

                const updatedTagsByDocument = previousTagsData.knowledgeBaseDocumentTagsByDocument.map((entry) =>
                    entry.knowledgeBaseDocumentId === knowledgeBaseDocumentId
                        ? {...entry, tags: tagsWithTempIds}
                        : entry
                );

                const documentHasTagsEntry = previousTagsData.knowledgeBaseDocumentTagsByDocument.some(
                    (entry) => entry.knowledgeBaseDocumentId === knowledgeBaseDocumentId
                );

                return documentHasTagsEntry
                    ? {...previousTagsData, knowledgeBaseDocumentTagsByDocument: updatedTagsByDocument}
                    : {
                          ...previousTagsData,
                          knowledgeBaseDocumentTagsByDocument: [
                              ...previousTagsData.knowledgeBaseDocumentTagsByDocument,
                              {knowledgeBaseDocumentId, tags: tagsWithTempIds},
                          ],
                      };
            })();

            queryClient.setQueryData(['knowledgeBaseDocumentTagsByDocument'], optimisticTagsData);

            return {cachedTagsByDocumentData: previousTagsData};
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
